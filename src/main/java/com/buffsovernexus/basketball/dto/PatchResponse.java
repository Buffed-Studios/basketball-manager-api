package com.buffsovernexus.basketball.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PatchResponse(
        UUID id,
        String title,
        String version,
        String notes,
        OffsetDateTime createdAt
) {}

