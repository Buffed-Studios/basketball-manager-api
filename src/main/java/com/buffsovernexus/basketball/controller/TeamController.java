package com.buffsovernexus.basketball.controller;

import com.buffsovernexus.basketball.dto.TeamRosterResponse;
import com.buffsovernexus.basketball.dto.TeamSummaryResponse;
import com.buffsovernexus.basketball.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/scenarios/{scenarioId}/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    public ResponseEntity<List<TeamSummaryResponse>> getAll(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID scenarioId
    ) {
        return ResponseEntity.ok(teamService.getTeams(user.getUsername(), scenarioId));
    }

    @GetMapping("/{teamId}/roster")
    public ResponseEntity<TeamRosterResponse> getRoster(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID scenarioId,
            @PathVariable UUID teamId
    ) {
        return ResponseEntity.ok(teamService.getTeamRoster(user.getUsername(), scenarioId, teamId));
    }
}

