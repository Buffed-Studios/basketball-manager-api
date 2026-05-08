package com.buffsovernexus.basketball.dto;

import com.buffsovernexus.basketball.entity.GamePhase;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ScenarioResponse(
        UUID id,
        String name,
        GamePhase currentPhase,
        int currentYear,
        OffsetDateTime createdAt,
        TeamSummaryResponse userTeam
) {}

