package com.buffsovernexus.basketball.dto;

import java.util.UUID;

public record TeamSummaryResponse(
        UUID id,
        String name,
        long budget,
        boolean userTeam
) {}

