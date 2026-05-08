package com.buffsovernexus.basketball.dto;

import com.buffsovernexus.basketball.entity.Position;

import java.util.UUID;

public record PlayerSummaryResponse(
        UUID id,
        String firstName,
        String lastName,
        Position position,
        int heightInches,
        int age,
        boolean benched,
        boolean freeAgent,
        long cost
) {}

