package com.buffsovernexus.basketball.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GameResponse(
        UUID id,
        UUID homeTeamId,
        String homeTeamName,
        UUID awayTeamId,
        String awayTeamName,
        int yearNumber,
        int gameNumber,
        boolean played,
        Integer homeScore,
        Integer awayScore,
        OffsetDateTime playedAt
) {}

