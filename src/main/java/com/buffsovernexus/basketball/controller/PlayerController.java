package com.buffsovernexus.basketball.controller;

import com.buffsovernexus.basketball.dto.PlayerDetailResponse;
import com.buffsovernexus.basketball.dto.PlayerSeasonStatsResponse;
import com.buffsovernexus.basketball.dto.PlayerSummaryResponse;
import com.buffsovernexus.basketball.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/scenarios/{scenarioId}")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    // GET /api/scenarios/{scenarioId}/free-agents
    @GetMapping("/free-agents")
    public ResponseEntity<List<PlayerSummaryResponse>> getFreeAgents(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID scenarioId
    ) {
        return ResponseEntity.ok(playerService.getFreeAgents(user.getUsername(), scenarioId));
    }

    // GET /api/scenarios/{scenarioId}/players/{playerId}
    @GetMapping("/players/{playerId}")
    public ResponseEntity<PlayerDetailResponse> getPlayer(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID scenarioId,
            @PathVariable UUID playerId
    ) {
        return ResponseEntity.ok(playerService.getPlayer(user.getUsername(), scenarioId, playerId));
    }

    // GET /api/scenarios/{scenarioId}/players/{playerId}/stats
    @GetMapping("/players/{playerId}/stats")
    public ResponseEntity<List<PlayerSeasonStatsResponse>> getPlayerStats(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID scenarioId,
            @PathVariable UUID playerId
    ) {
        return ResponseEntity.ok(playerService.getPlayerStats(user.getUsername(), scenarioId, playerId));
    }
}

