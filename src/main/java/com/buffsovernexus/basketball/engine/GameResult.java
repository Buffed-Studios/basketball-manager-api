package com.buffsovernexus.basketball.engine;

import java.util.List;
import java.util.UUID;

/**
 * Immutable result returned after a game completes.
 */
public record GameResult(
        UUID gameId,
        String homeName,
        int homeScore,
        String awayName,
        int awayScore,
        String winner,
        List<PlayerStats> playerStats
) {
    public record PlayerStats(
            UUID playerId,
            UUID teamId,
            String playerName,
            int twoPointAttempts,
            int twoPointMade,
            int fourPointAttempts,
            int fourPointMade,
            int freeThrowAttempts,
            int freeThrowMade,
            int passesAttempted,
            int passesCompleted,
            int stealsAttempted,
            int stealsMade,
            int blocksAttempted,
            int blocksMade,
            int reboundsAttempted,
            int reboundsMade,
            int totalPoints,
            int possessionWins,
            int possessionLosses
    ) {}
}

