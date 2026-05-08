package com.buffsovernexus.basketball.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePatchRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 50)  String version,
        @NotBlank                  String notes
) {}

