package com.buffsovernexus.basketball.service;

import com.buffsovernexus.basketball.dto.CreateScenarioRequest;
import com.buffsovernexus.basketball.dto.ScenarioResponse;
import com.buffsovernexus.basketball.dto.TeamSummaryResponse;
import com.buffsovernexus.basketball.entity.*;
import com.buffsovernexus.basketball.exception.ResourceNotFoundException;
import com.buffsovernexus.basketball.exception.ScenarioNameAlreadyExistsException;
import com.buffsovernexus.basketball.repository.AccountRepository;
import com.buffsovernexus.basketball.repository.PlayerRepository;
import com.buffsovernexus.basketball.repository.ScenarioRepository;
import com.buffsovernexus.basketball.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final AccountRepository accountRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final GameService gameService;

    private final Random random = new Random();

    // Budget configuration - balanced for strategic team building
    // With this budget, teams can afford 2 prime players (~80M each) plus role players
    private static final long TEAM_STARTING_BUDGET = 150_000_000L; // 150M per team

    @Transactional
    public ScenarioResponse createScenario(String username, CreateScenarioRequest request) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (scenarioRepository.existsByAccountIdAndName(account.getId(), request.name())) {
            throw new ScenarioNameAlreadyExistsException(request.name());
        }

        Scenario scenario = Scenario.builder()
                .account(account)
                .name(request.name())
                .currentPhase(GamePhase.REGULAR_SEASON) // Skip directly to regular season
                .currentYear(1)
                .build();

        scenarioRepository.save(scenario);

        // Create user team using scenario name
        Team userTeam = Team.builder()
                .scenario(scenario)
                .name(request.name())
                .budget(TEAM_STARTING_BUDGET)
                .userTeam(true)
                .build();

        // Create 7 bot teams
        String[] botNames = {
                "Rim Rattlers", "Net Blazers", "Court Kings",
                "Sky Dunkers", "Paint Crushers", "Arc Snipers", "Fast Breakers"
        };

        List<Team> allTeams = new ArrayList<>();
        allTeams.add(userTeam);
        teamRepository.save(userTeam);

        for (String botName : botNames) {
            Team botTeam = Team.builder()
                    .scenario(scenario)
                    .name(botName)
                    .budget(TEAM_STARTING_BUDGET)
                    .userTeam(false)
                    .build();
            teamRepository.save(botTeam);
            allTeams.add(botTeam);
        }

        // Generate players for each team (1 Guard and 1 Forward per team)
        for (Team team : allTeams) {
            generatePlayerForTeam(scenario, team, Position.GUARD);
            generatePlayerForTeam(scenario, team, Position.FORWARD);
        }

        // Generate full season schedule (round-robin: each team plays every other team twice)
        // For 8 teams: 8 × 7 × 2 = 112 total games
        gameService.generateInitialSeasonSchedule(scenario, allTeams, scenario.getCurrentYear());

        return toResponse(scenario, userTeam);
    }

    @Transactional(readOnly = true)
    public List<ScenarioResponse> getScenarios(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        return scenarioRepository.findAllByAccountId(account.getId()).stream()
                .map(s -> {
                    Team userTeam = teamRepository.findByScenarioIdAndUserTeamTrue(s.getId()).orElse(null);
                    return toResponse(s, userTeam);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ScenarioResponse getScenario(String username, UUID scenarioId) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        Scenario scenario = scenarioRepository.findByIdAndAccountId(scenarioId, account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Scenario not found"));

        Team userTeam = teamRepository.findByScenarioIdAndUserTeamTrue(scenario.getId()).orElse(null);
        return toResponse(scenario, userTeam);
    }

    @Transactional
    public void deleteScenario(String username, UUID scenarioId) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        Scenario scenario = scenarioRepository.findByIdAndAccountId(scenarioId, account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Scenario not found"));

        scenarioRepository.delete(scenario);
    }

    /**
     * Generates a player for a team with randomized attributes.
     * Creates a balanced player with skills, growth potential, and longevity.
     */
    private void generatePlayerForTeam(Scenario scenario, Team team, Position position) {
        String[] firstNames = {
                "James", "Michael", "Kevin", "Stephen", "LeBron", "Kobe", "Magic", "Larry",
                "Shaquille", "Tim", "Hakeem", "Kareem", "Wilt", "Bill", "Oscar", "Jerry",
                "Dwyane", "Chris", "Allen", "Ray", "Paul", "Dirk", "Jason", "Grant"
        };
        String[] lastNames = {
                "Johnson", "Williams", "Brown", "Jones", "Davis", "Miller", "Wilson", "Moore",
                "Taylor", "Anderson", "Thomas", "Jackson", "White", "Harris", "Martin", "Thompson",
                "Garcia", "Martinez", "Robinson", "Clark", "Rodriguez", "Lewis", "Lee", "Walker"
        };

        String firstName = firstNames[random.nextInt(firstNames.length)];
        String lastName = lastNames[random.nextInt(lastNames.length)];

        // Age: 20-28 for active players
        int age = 20 + random.nextInt(9);

        // Height: Guards 72-78 inches (6'0" - 6'6"), Forwards 78-84 inches (6'6" - 7'0")
        int heightInches = position == Position.GUARD
                ? 72 + random.nextInt(7)
                : 78 + random.nextInt(7);

        // Potential: 0-20 (how much they can still grow)
        int potential = random.nextInt(21);
        int potentialRemaining = potential;

        // Growth: 1-5 (how fast they improve)
        int growth = 1 + random.nextInt(5);

        // Longevity: 5-15 years (how long they stay in prime)
        int longevity = 5 + random.nextInt(11);
        int longevityRemaining = longevity;

        // Decay: 1-5 (how fast they decline after prime)
        int decay = 1 + random.nextInt(5);

        Player player = Player.builder()
                .scenario(scenario)
                .firstName(firstName)
                .lastName(lastName)
                .position(position)
                .heightInches(heightInches)
                .age(age)
                .potential(potential)
                .potentialRemaining(potentialRemaining)
                .growth(growth)
                .longevity(longevity)
                .longevityRemaining(longevityRemaining)
                .decay(decay)
                .currentTeam(team)
                .originalTeam(team)
                .benched(false)
                .freeAgent(false)
                .skills(new ArrayList<>())
                .build();

        playerRepository.save(player);

        // Generate skills (30-70 range for balanced gameplay)
        for (SkillType skillType : SkillType.values()) {
            int skillValue = 30 + random.nextInt(41); // 30-70 range

            // Position-based skill adjustments
            if (position == Position.GUARD) {
                // Guards better at shooting and passing
                if (skillType == SkillType.FOUR_POINT || skillType == SkillType.PASSING) {
                    skillValue = Math.min(100, skillValue + random.nextInt(15)); // Boost by 0-14, cap at 100
                }
            } else {
                // Forwards better at rebounding and blocking
                if (skillType == SkillType.REBOUNDING || skillType == SkillType.BLOCKING) {
                    skillValue = Math.min(100, skillValue + random.nextInt(15)); // Boost by 0-14, cap at 100
                }
            }

            PlayerSkill skill = PlayerSkill.builder()
                    .player(player)
                    .skillType(skillType)
                    .value(skillValue)
                    .build();

            player.getSkills().add(skill);
        }

        playerRepository.save(player);
    }

    private ScenarioResponse toResponse(Scenario scenario, Team userTeam) {
        TeamSummaryResponse userTeamSummary = userTeam != null
                ? new TeamSummaryResponse(userTeam.getId(), userTeam.getName(), userTeam.getBudget(), true)
                : null;

        return new ScenarioResponse(
                scenario.getId(),
                scenario.getName(),
                scenario.getCurrentPhase(),
                scenario.getCurrentYear(),
                scenario.getCreatedAt(),
                userTeamSummary
        );
    }
}

