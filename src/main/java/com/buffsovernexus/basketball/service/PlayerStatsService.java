package com.buffsovernexus.basketball.service;

import com.buffsovernexus.basketball.engine.GameResult;
import com.buffsovernexus.basketball.entity.Player;
import com.buffsovernexus.basketball.entity.PlayerSeasonStats;
import com.buffsovernexus.basketball.entity.Scenario;
import com.buffsovernexus.basketball.entity.Team;
import com.buffsovernexus.basketball.exception.ResourceNotFoundException;
import com.buffsovernexus.basketball.repository.PlayerRepository;
import com.buffsovernexus.basketball.repository.PlayerSeasonStatsRepository;
import com.buffsovernexus.basketball.repository.ScenarioRepository;
import com.buffsovernexus.basketball.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Handles persistence of player season stats after a game finishes.
 * Upserts a {@link PlayerSeasonStats} row per player, accumulating
 * all counters from the just-completed game.
 */
@Service
@RequiredArgsConstructor
public class PlayerStatsService {

    private final PlayerSeasonStatsRepository statsRepository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final ScenarioRepository scenarioRepository;

    /**
     * Called after any game (live, watched, or simulated) finishes.
     * Iterates over each player's in-game stats and merges them into
     * the running season totals for the given scenario/year.
     *
     * @param result     the completed game result containing per-player stats
     * @param scenarioId the scenario the game belongs to
     * @param yearNumber the season year the game was played in
     */
    @Transactional
    public void updateSeasonStats(GameResult result, UUID scenarioId, int yearNumber) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Scenario not found: " + scenarioId));

        for (GameResult.PlayerStats ps : result.playerStats()) {
            Player player = playerRepository.findById(ps.playerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + ps.playerId()));
            Team team = teamRepository.findById(ps.teamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + ps.teamId()));

            PlayerSeasonStats stats = statsRepository
                    .findByPlayerIdAndScenarioIdAndYearNumberAndTeamId(
                            ps.playerId(), scenarioId, yearNumber, ps.teamId())
                    .orElseGet(() -> PlayerSeasonStats.builder()
                            .player(player)
                            .scenario(scenario)
                            .team(team)
                            .yearNumber(yearNumber)
                            .gamesPlayed(0)
                            .twoPointAttempts(0)
                            .twoPointMade(0)
                            .fourPointAttempts(0)
                            .fourPointMade(0)
                            .freeThrowAttempts(0)
                            .freeThrowMade(0)
                            .passesAttempted(0)
                            .passesCompleted(0)
                            .stealsAttempted(0)
                            .stealsMade(0)
                            .blocksAttempted(0)
                            .blocksMade(0)
                            .reboundsAttempted(0)
                            .reboundsMade(0)
                            .totalPoints(0)
                            .possessionWins(0)
                            .possessionLosses(0)
                            .build());

            stats.setGamesPlayed(stats.getGamesPlayed() + 1);
            stats.setTwoPointAttempts(stats.getTwoPointAttempts()   + ps.twoPointAttempts());
            stats.setTwoPointMade(stats.getTwoPointMade()           + ps.twoPointMade());
            stats.setFourPointAttempts(stats.getFourPointAttempts() + ps.fourPointAttempts());
            stats.setFourPointMade(stats.getFourPointMade()         + ps.fourPointMade());
            stats.setFreeThrowAttempts(stats.getFreeThrowAttempts() + ps.freeThrowAttempts());
            stats.setFreeThrowMade(stats.getFreeThrowMade()         + ps.freeThrowMade());
            stats.setPassesAttempted(stats.getPassesAttempted()     + ps.passesAttempted());
            stats.setPassesCompleted(stats.getPassesCompleted()     + ps.passesCompleted());
            stats.setStealsAttempted(stats.getStealsAttempted()     + ps.stealsAttempted());
            stats.setStealsMade(stats.getStealsMade()               + ps.stealsMade());
            stats.setBlocksAttempted(stats.getBlocksAttempted()     + ps.blocksAttempted());
            stats.setBlocksMade(stats.getBlocksMade()               + ps.blocksMade());
            stats.setReboundsAttempted(stats.getReboundsAttempted() + ps.reboundsAttempted());
            stats.setReboundsMade(stats.getReboundsMade()           + ps.reboundsMade());
            stats.setTotalPoints(stats.getTotalPoints()             + ps.totalPoints());
            stats.setPossessionWins(stats.getPossessionWins()       + ps.possessionWins());
            stats.setPossessionLosses(stats.getPossessionLosses()   + ps.possessionLosses());

            statsRepository.save(stats);
        }
    }
}

