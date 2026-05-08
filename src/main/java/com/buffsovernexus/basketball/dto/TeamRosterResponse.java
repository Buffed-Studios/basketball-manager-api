package com.buffsovernexus.basketball.dto;

import java.util.List;
import java.util.UUID;

public record TeamRosterResponse(
        UUID id,
        String name,
        long budget,
        boolean userTeam,
        List<PlayerSummaryResponse> starters,
        List<PlayerSummaryResponse> bench
) {}

