package com.buffsovernexus.basketball.controller;

import com.buffsovernexus.basketball.dto.CreateScenarioRequest;
import com.buffsovernexus.basketball.dto.ScenarioResponse;
import com.buffsovernexus.basketball.service.ScenarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/scenarios")
@RequiredArgsConstructor
public class ScenarioController {

    private final ScenarioService scenarioService;

    @PostMapping
    public ResponseEntity<ScenarioResponse> create(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody CreateScenarioRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scenarioService.createScenario(user.getUsername(), request));
    }

    @GetMapping
    public ResponseEntity<List<ScenarioResponse>> getAll(
            @AuthenticationPrincipal UserDetails user
    ) {
        return ResponseEntity.ok(scenarioService.getScenarios(user.getUsername()));
    }

    @GetMapping("/{scenarioId}")
    public ResponseEntity<ScenarioResponse> getOne(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID scenarioId
    ) {
        return ResponseEntity.ok(scenarioService.getScenario(user.getUsername(), scenarioId));
    }

    @DeleteMapping("/{scenarioId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID scenarioId
    ) {
        scenarioService.deleteScenario(user.getUsername(), scenarioId);
        return ResponseEntity.noContent().build();
    }
}

