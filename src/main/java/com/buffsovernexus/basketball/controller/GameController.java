package com.buffsovernexus.basketball.controller;

import com.buffsovernexus.basketball.dto.GameResponse;
import com.buffsovernexus.basketball.dto.SeasonScheduleResponse;
import com.buffsovernexus.basketball.dto.TeamStandingResponse;
import com.buffsovernexus.basketball.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/scenarios/{scenarioId}/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    /**
     * POST /api/scenarios/{scenarioId}/games/start
     * Body: { "homeTeamId": "...", "awayTeamId": "..." }
     *
     * Returns the gameId. Client should then subscribe to:
     *   /topic/game/{gameId}
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startGame(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID scenarioId,
            @RequestBody Map<String, UUID> body
    ) {
        UUID homeTeamId = body.get("homeTeamId");
        UUID awayTeamId = body.get("awayTeamId");

        UUID gameId = gameService.startGame(user.getUsername(), scenarioId, homeTeamId, awayTeamId);

        return ResponseEntity.accepted().body(Map.of(
                "gameId", gameId.toString(),
                "topic", "/topic/game/" + gameId
        ));
    }

    /**
     * POST /api/scenarios/{scenarioId}/games/season/{yearNumber}/generate
     *
     * Generates a full regular season schedule (round-robin format).
     * Each team plays every other team twice (home and away).
     * For 8 teams: 8 × 7 × 2 = 112 total games.
     */
    @PostMapping("/season/{yearNumber}/generate")
    public ResponseEntity<SeasonScheduleResponse> generateSeasonSchedule(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID scenarioId,
            @PathVariable int yearNumber
    ) {
        SeasonScheduleResponse response = gameService.generateSeasonSchedule(
                user.getUsername(), scenarioId, yearNumber
        );
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/scenarios/{scenarioId}/games/season/{yearNumber}
     *
     * Gets the schedule for a specific season year.
     */
    @GetMapping("/season/{yearNumber}")
    public ResponseEntity<SeasonScheduleResponse> getSeasonSchedule(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID scenarioId,
            @PathVariable int yearNumber
    ) {
        SeasonScheduleResponse response = gameService.getSeasonSchedule(
                user.getUsername(), scenarioId, yearNumber
        );
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/scenarios/{scenarioId}/games/season/{yearNumber}/standings
     *
     * Returns standings derived from played games for the given year.
     */
    @GetMapping("/season/{yearNumber}/standings")
    public ResponseEntity<List<TeamStandingResponse>> getStandings(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID scenarioId,
            @PathVariable int yearNumber
    ) {
        return ResponseEntity.ok(gameService.getStandings(user.getUsername(), scenarioId, yearNumber));
    }

    /**
     * POST /api/scenarios/{scenarioId}/games/{gameId}/simulate
     *
     * Instantly simulates a scheduled game without WebSocket streaming.
     * Persists the result and returns the updated game.
     */
    @PostMapping("/{gameId}/simulate")
    public ResponseEntity<GameResponse> simulateGame(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID scenarioId,
            @PathVariable UUID gameId
    ) {
        return ResponseEntity.ok(gameService.simulateScheduledGame(user.getUsername(), scenarioId, gameId));
    }

    /**
     * POST /api/scenarios/{scenarioId}/games/{gameId}/watch
     *
     * Starts live WebSocket streaming of a scheduled game.
     * Returns {gameId, topic} so the client can subscribe to /topic/game/{gameId}.
     */
    @PostMapping("/{gameId}/watch")
    public ResponseEntity<Map<String, String>> watchGame(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID scenarioId,
            @PathVariable UUID gameId
    ) {
        return ResponseEntity.accepted().body(gameService.watchScheduledGame(user.getUsername(), scenarioId, gameId));
    }

    /**
     * POST /api/scenarios/{scenarioId}/games/{gameId}/skip
     *
     * Fast-forwards a currently-running watched game to its conclusion.
     * All remaining play-by-play events are suppressed; a single GAME_END event
     * is broadcast on /topic/game/{gameId} with the final score.
     * The game result is persisted and standings are updated exactly as they
     * would be if the game had played out normally.
     *
     * Returns 200 if the signal was delivered, or 404 if no live game with
     * that ID is currently running (already finished or never started).
     */
    @PostMapping("/{gameId}/skip")
    public ResponseEntity<Map<String, String>> skipGame(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID scenarioId,
            @PathVariable UUID gameId
    ) {
        boolean found = gameService.skipGameToEnd(user.getUsername(), scenarioId, gameId);
        if (!found) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "No live game found with id " + gameId +
                              ". The game may have already finished or was never started with /watch."
            ));
        }
        return ResponseEntity.ok(Map.of(
                "status", "skipping",
                "gameId", gameId.toString(),
                "topic",  "/topic/game/" + gameId
        ));
    }
}

