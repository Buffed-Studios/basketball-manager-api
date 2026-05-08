package com.buffsovernexus.basketball.dto;

import java.util.UUID;

public record PlayerSeasonStatsResponse(
        UUID id,
        int yearNumber,
        String teamName,
        int gamesPlayed,
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

