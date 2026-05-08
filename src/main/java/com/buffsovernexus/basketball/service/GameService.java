package com.buffsovernexus.basketball.service;

import com.buffsovernexus.basketball.dto.GameResponse;
import com.buffsovernexus.basketball.dto.SeasonScheduleResponse;
import com.buffsovernexus.basketball.dto.TeamStandingResponse;
import com.buffsovernexus.basketball.engine.GameEngine;
import com.buffsovernexus.basketball.engine.GamePlayer;
import com.buffsovernexus.basketball.engine.GameResult;
import com.buffsovernexus.basketball.engine.GameTeam;
import com.buffsovernexus.basketball.entity.*;
import com.buffsovernexus.basketball.exception.ResourceNotFoundException;
import com.buffsovernexus.basketball.exception.RosterViolationException;
import com.buffsovernexus.basketball.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameService {

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final ScenarioRepository scenarioRepository;
    private final AccountRepository accountRepository;
    private final GameRepository gameRepository;
    private final SimpMessagingTemplate messaging;
    private final GameExecutionService gameExecutionService;
    private final PlayerStatsService playerStatsService;

    /**
     * Validates rosters, builds GameTeams, and launches the engine asynchronously.
     * The game runs in a background thread, broadcasting events over WebSocket.
     * Returns the gameId (UUID) so the client can subscribe to the right topic.
     * Player season stats are persisted once the async game finishes.
     */
    @Transactional(readOnly = true)
    public UUID startGame(String username, UUID scenarioId, UUID homeTeamId, UUID awayTeamId) {
        accountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Scenario not found"));

        GameTeam homeTeam = buildGameTeam(homeTeamId, scenarioId);
        GameTeam awayTeam = buildGameTeam(awayTeamId, scenarioId);

        UUID gameId = UUID.randomUUID();
        String topic = "/topic/game/" + gameId;

        gameExecutionService.runGameAsync(gameId, homeTeam, awayTeam, topic,
                scenarioId, scenario.getCurrentYear());

        return gameId;
    }

    /**
     * Instantly simulates a scheduled game without WebSocket streaming.
     * Runs the engine in fast mode (no delays or broadcasts), persists the result,
     * and returns the updated GameResponse.
     */
    @Transactional
    public GameResponse simulateScheduledGame(String username, UUID scenarioId, UUID gameId) {
        accountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found"));

        if (game.isPlayed()) {
            throw new IllegalStateException("Game has already been played");
        }

        GameTeam homeTeam = buildGameTeam(game.getHomeTeam().getId(), scenarioId);
        GameTeam awayTeam = buildGameTeam(game.getAwayTeam().getId(), scenarioId);

        // fast mode = no delays, no WebSocket broadcasts
        GameEngine engine = new GameEngine(gameId, homeTeam, awayTeam, messaging, null, true);
        GameResult result = engine.run();

        game.setPlayed(true);
        game.setHomeScore(result.homeScore());
        game.setAwayScore(result.awayScore());
        game.setPlayedAt(OffsetDateTime.now());
        gameRepository.save(game);

        // Persist player season stats — winner/loser implicit in stored scores.
        playerStatsService.updateSeasonStats(result, scenarioId, game.getYearNumber());

        return toGameResponse(game);
    }

    /**
     * Starts a live watch of a scheduled game via WebSocket.
     * The game runs asynchronously on a background thread and broadcasts events
     * to /topic/game/{gameId}. The result is persisted when the game ends.
     * Returns {gameId, topic} so the client knows where to subscribe.
     */
    @Transactional(readOnly = true)
    public Map<String, String> watchScheduledGame(String username, UUID scenarioId, UUID gameId) {
        accountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found"));

        if (game.isPlayed()) {
            throw new IllegalStateException("Game has already been played");
        }

        GameTeam homeTeam = buildGameTeam(game.getHomeTeam().getId(), scenarioId);
        GameTeam awayTeam = buildGameTeam(game.getAwayTeam().getId(), scenarioId);

        String topic = "/topic/game/" + gameId;
        gameExecutionService.runScheduledGameAsync(gameId, homeTeam, awayTeam, topic,
                scenarioId, game.getYearNumber());

        return Map.of("gameId", gameId.toString(), "topic", topic);
    }

    /**
     * Signals a currently-running watched game to skip all remaining play-by-play
     * and race to the final result. A GAME_END event is still broadcast so the UI
     * receives the final score. The game result is persisted as normal.
     *
     * Returns {@code true} if the game was found and signalled, {@code false} if
     * no live game with that ID is currently running (already finished or not started).
     */
    @Transactional(readOnly = true)
    public boolean skipGameToEnd(String username, UUID scenarioId, UUID gameId) {
        accountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        gameRepository.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found"));

        return gameExecutionService.skipGameToEnd(gameId);
    }


    /**
     * Generates a full regular season schedule for the given scenario and year.
     * Uses a round-robin algorithm where each team plays every other team twice (home and away).
     * For 8 teams, this results in: 8 teams × 7 opponents × 2 games = 112 total games per season.
     */
    @Transactional
    public SeasonScheduleResponse generateSeasonSchedule(String username, UUID scenarioId, int yearNumber) {
        accountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Scenario not found"));

        // Check if season already has games
        long existingGames = gameRepository.countByScenarioIdAndYearNumber(scenarioId, yearNumber);
        if (existingGames > 0) {
            throw new IllegalStateException("Season " + yearNumber + " already has games scheduled");
        }

        // Get all teams for this scenario
        List<Team> teams = teamRepository.findAllByScenarioId(scenarioId);
        if (teams.size() < 2) {
            throw new IllegalStateException("Need at least 2 teams to generate a season");
        }

        // Generate round-robin schedule
        List<Game> scheduledGames = generateRoundRobinSchedule(scenario, teams, yearNumber);
        gameRepository.saveAll(scheduledGames);

        // Convert to response
        List<GameResponse> gameResponses = scheduledGames.stream()
                .map(this::toGameResponse)
                .collect(Collectors.toList());

        return new SeasonScheduleResponse(yearNumber, scheduledGames.size(), gameResponses);
    }

    /**
     * Gets the schedule for a specific season year.
     */
    @Transactional(readOnly = true)
    public SeasonScheduleResponse getSeasonSchedule(String username, UUID scenarioId, int yearNumber) {
        accountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Scenario not found"));

        List<Game> games = gameRepository.findAllByScenarioIdAndYearNumberOrderByGameNumber(scenarioId, yearNumber);

        List<GameResponse> gameResponses = games.stream()
                .map(this::toGameResponse)
                .collect(Collectors.toList());

        return new SeasonScheduleResponse(yearNumber, games.size(), gameResponses);
    }

    /**
     * Computes standings for a given season year by aggregating played game results.
     * Teams are ranked by wins (desc), then point differential (desc).
     */
    @Transactional(readOnly = true)
    public List<TeamStandingResponse> getStandings(String username, UUID scenarioId, int yearNumber) {
        accountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Scenario not found"));

        List<Team> teams = teamRepository.findAllByScenarioId(scenarioId);
        List<Game> games = gameRepository.findAllByScenarioIdAndYearNumberOrderByGameNumber(scenarioId, yearNumber);

        // Initialise stat buckets per team
        Map<UUID, int[]> stats = new HashMap<>(); // [gp, wins, losses, pf, pa]
        for (Team t : teams) {
            stats.put(t.getId(), new int[]{0, 0, 0, 0, 0});
        }

        for (Game g : games) {
            if (!g.isPlayed() || g.getHomeScore() == null || g.getAwayScore() == null) continue;

            int hs = g.getHomeScore();
            int as = g.getAwayScore();
            UUID homeId = g.getHomeTeam().getId();
            UUID awayId = g.getAwayTeam().getId();

            int[] home = stats.getOrDefault(homeId, new int[5]);
            int[] away = stats.getOrDefault(awayId, new int[5]);

            home[0]++; away[0]++; // games played
            home[3] += hs; home[4] += as; // points for / against (home)
            away[3] += as; away[4] += hs; // points for / against (away)

            if (hs > as) { home[1]++; away[2]++; }
            else          { away[1]++; home[2]++; }

            stats.put(homeId, home);
            stats.put(awayId, away);
        }

        // Build list and sort: wins desc, then point-diff desc
        List<TeamStandingResponse> rows = new ArrayList<>();
        for (Team t : teams) {
            int[] s = stats.getOrDefault(t.getId(), new int[5]);
            int diff = s[3] - s[4];
            rows.add(new TeamStandingResponse(0, t.getId(), t.getName(), t.isUserTeam(),
                    s[0], s[1], s[2], s[3], s[4], diff));
        }

        rows.sort(Comparator.comparingInt(TeamStandingResponse::wins).reversed()
                .thenComparingInt(TeamStandingResponse::pointDifferential).reversed());

        // Assign ranks
        List<TeamStandingResponse> ranked = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            TeamStandingResponse r = rows.get(i);
            ranked.add(new TeamStandingResponse(i + 1, r.teamId(), r.teamName(), r.userTeam(),
                    r.gamesPlayed(), r.wins(), r.losses(), r.pointsFor(), r.pointsAgainst(), r.pointDifferential()));
        }
        return ranked;
    }

    /**
     * Internal method to generate games for a scenario during creation.
     * This is called without authentication checks since it's part of scenario setup.
     */
    @Transactional
    public void generateInitialSeasonSchedule(Scenario scenario, List<Team> teams, int yearNumber) {
        List<Game> scheduledGames = generateRoundRobinSchedule(scenario, teams, yearNumber);
        gameRepository.saveAll(scheduledGames);
    }

    // -------------------------------------------------------------------------
    // Schedule Generation Logic
    // -------------------------------------------------------------------------

    /**
     * Generates a round-robin schedule where each team plays every other team twice.
     * The schedule is shuffled to add variety.
     */
    private List<Game> generateRoundRobinSchedule(Scenario scenario, List<Team> teams, int yearNumber) {
        List<Game> games = new ArrayList<>();
        int gameNumber = 1;

        // Each team plays every other team twice (home and away)
        for (int i = 0; i < teams.size(); i++) {
            for (int j = 0; j < teams.size(); j++) {
                if (i != j) {
                    // Create home game for team i vs team j
                    Game game = Game.builder()
                            .scenario(scenario)
                            .homeTeam(teams.get(i))
                            .awayTeam(teams.get(j))
                            .yearNumber(yearNumber)
                            .gameNumber(gameNumber++)
                            .played(false)
                            .build();
                    games.add(game);
                }
            }
        }

        // Shuffle to add variety to the schedule
        Collections.shuffle(games);

        // Reassign game numbers after shuffle
        for (int i = 0; i < games.size(); i++) {
            games.get(i).setGameNumber(i + 1);
        }

        return games;
    }

    private GameResponse toGameResponse(Game game) {
        return new GameResponse(
                game.getId(),
                game.getHomeTeam().getId(),
                game.getHomeTeam().getName(),
                game.getAwayTeam().getId(),
                game.getAwayTeam().getName(),
                game.getYearNumber(),
                game.getGameNumber(),
                game.isPlayed(),
                game.getHomeScore(),
                game.getAwayScore(),
                game.getPlayedAt()
        );
    }

    // -------------------------------------------------------------------------
    // Team builder
    // -------------------------------------------------------------------------

    private GameTeam buildGameTeam(UUID teamId, UUID scenarioId) {
        Team team = teamRepository.findByIdAndScenarioId(teamId, scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + teamId));

        List<Player> players = playerRepository.findAllByCurrentTeamId(teamId);

        // Filter starters only (not benched)
        List<Player> guards = players.stream()
                .filter(p -> p.getPosition() == Position.GUARD && !p.isBenched())
                .toList();

        List<Player> forwards = players.stream()
                .filter(p -> p.getPosition() == Position.FORWARD && !p.isBenched())
                .toList();

        if (guards.isEmpty()) {
            throw new RosterViolationException(team.getName() + " has no active Guard.");
        }
        if (forwards.isEmpty()) {
            throw new RosterViolationException(team.getName() + " has no active Forward.");
        }

        GamePlayer guard   = new GamePlayer(guards.get(0));
        GamePlayer forward = new GamePlayer(forwards.get(0));

        return new GameTeam(team, guard, forward);
    }
}

