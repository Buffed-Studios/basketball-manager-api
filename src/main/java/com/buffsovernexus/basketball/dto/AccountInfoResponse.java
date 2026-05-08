package com.buffsovernexus.basketball.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountInfoResponse(
        UUID id,
        String username,
        OffsetDateTime createdAt
) {}

