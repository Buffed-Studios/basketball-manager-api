package com.buffsovernexus.basketball.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateScenarioRequest(
        @NotBlank(message = "Scenario name is required")
        @Size(min = 2, max = 100, message = "Scenario name must be between 2 and 100 characters")
        String name
) {}

