package com.buffsovernexus.basketball.service;

import com.buffsovernexus.basketball.engine.GameEngine;
import com.buffsovernexus.basketball.engine.GameResult;
import com.buffsovernexus.basketball.engine.GameTeam;
import com.buffsovernexus.basketball.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class GameExecutionService {

    private final SimpMessagingTemplate messaging;
    private final GameRepository gameRepository;
    private final PlayerStatsService playerStatsService;

    /** Live engines keyed by their gameId — used to fast-forward on demand. */
    private final ConcurrentHashMap<UUID, GameEngine> runningGames = new ConcurrentHashMap<>();

    /**
     * Ad-hoc game (not tied to a scheduled season game).
     * Persists player season stats but does NOT create a game record,
     * so team standings from scheduled games remain unaffected.
     */
    @Async
    public void runGameAsync(UUID gameId, GameTeam home, GameTeam away, String topic,
                             UUID scenarioId, int yearNumber) {
        // Give the client a short window to subscribe before the first broadcast.
        sleepQuietly(250);

        GameEngine engine = new GameEngine(gameId, home, away, messaging, topic);
        runningGames.put(gameId, engine);
        try {
            GameResult result = engine.run();

            // Persist player season stats so career/season tables stay current.
            playerStatsService.updateSeasonStats(result, scenarioId, yearNumber);
        } finally {
            runningGames.remove(gameId);
        }
    }

    /**
     * Watched scheduled game — persists game result, updates standings,
     * and accumulates player season stats.
     */
    @Async
    public void runScheduledGameAsync(UUID gameId, GameTeam home, GameTeam away, String topic,
                                      UUID scenarioId, int yearNumber) {
        // Give the client a short window to subscribe before the first broadcast.
        sleepQuietly(250);

        GameEngine engine = new GameEngine(gameId, home, away, messaging, topic);
        runningGames.put(gameId, engine);
        try {
            GameResult result = engine.run();

            // Persist the final score — this is what the standings query reads.
            gameRepository.findById(gameId).ifPresent(game -> {
                game.setPlayed(true);
                game.setHomeScore(result.homeScore());
                game.setAwayScore(result.awayScore());
                game.setPlayedAt(OffsetDateTime.now());
                gameRepository.save(game);
            });

            // Accumulate player season stats.
            playerStatsService.updateSeasonStats(result, scenarioId, yearNumber);
        } finally {
            runningGames.remove(gameId);
        }
    }

    /**
     * Signals the named game engine to finish instantly.
     * Returns {@code true} if the game was found and signalled,
     * {@code false} if no live game with that ID exists.
     */
    public boolean skipGameToEnd(UUID gameId) {
        GameEngine engine = runningGames.get(gameId);
        if (engine == null) return false;
        engine.triggerFastForward();
        return true;
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
