package com.buffsovernexus.basketball.dto;

import java.util.UUID;

public record TeamStandingResponse(
        int rank,
        UUID teamId,
        String teamName,
        boolean userTeam,
        int gamesPlayed,
        int wins,
        int losses,
        int pointsFor,
        int pointsAgainst,
        int pointDifferential
) {}

