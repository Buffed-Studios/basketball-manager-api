package com.buffsovernexus.basketball.dto;

import java.util.List;

public record SeasonScheduleResponse(
        int yearNumber,
        int totalGames,
        List<GameResponse> games
) {}

