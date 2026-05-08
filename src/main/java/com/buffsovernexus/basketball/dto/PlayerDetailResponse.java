package com.buffsovernexus.basketball.dto;

import com.buffsovernexus.basketball.entity.Position;
import com.buffsovernexus.basketball.entity.SkillType;

import java.util.Map;
import java.util.UUID;

public record PlayerDetailResponse(
        UUID id,
        String firstName,
        String lastName,
        Position position,
        int heightInches,
        int age,
        int potential,
        int potentialRemaining,
        int growth,
        int longevity,
        int longevityRemaining,
        int decay,
        boolean benched,
        boolean freeAgent,
        UUID currentTeamId,
        String currentTeamName,
        UUID originalTeamId,
        String originalTeamName,
        Map<SkillType, Integer> skills,
        long cost
) {}

