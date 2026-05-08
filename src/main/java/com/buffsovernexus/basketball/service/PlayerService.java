package com.buffsovernexus.basketball.service;

import com.buffsovernexus.basketball.dto.PlayerDetailResponse;
import com.buffsovernexus.basketball.dto.PlayerSeasonStatsResponse;
import com.buffsovernexus.basketball.dto.PlayerSummaryResponse;
import com.buffsovernexus.basketball.entity.*;
import com.buffsovernexus.basketball.exception.ResourceNotFoundException;
import com.buffsovernexus.basketball.repository.*;
import com.buffsovernexus.basketball.util.PlayerCostCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final ScenarioRepository scenarioRepository;
    private final AccountRepository accountRepository;
    private final TeamRepository teamRepository;
    private final PlayerSeasonStatsRepository playerSeasonStatsRepository;

    @Transactional(readOnly = true)
    public List<PlayerSummaryResponse> getFreeAgents(String username, UUID scenarioId) {
        validateScenarioOwnership(username, scenarioId);
        UUID userTeamId = getUserTeamId(scenarioId);

        return playerRepository.findAllByScenarioIdAndFreeAgentTrue(scenarioId).stream()
                .map(p -> toSummary(p, userTeamId))
                .toList();
    }

    @Transactional(readOnly = true)
    public PlayerDetailResponse getPlayer(String username, UUID scenarioId, UUID playerId) {
        validateScenarioOwnership(username, scenarioId);
        UUID userTeamId = getUserTeamId(scenarioId);

        Player player = playerRepository.findByIdAndScenarioId(playerId, scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found"));

        return toDetail(player, userTeamId);
    }

    @Transactional(readOnly = true)
    public List<PlayerSeasonStatsResponse> getPlayerStats(String username, UUID scenarioId, UUID playerId) {
        validateScenarioOwnership(username, scenarioId);

        playerRepository.findByIdAndScenarioId(playerId, scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found"));

        return playerSeasonStatsRepository.findAllByPlayerIdAndScenarioId(playerId, scenarioId).stream()
                .map(this::toStatsResponse)
                .toList();
    }

    // ---- Mapping helpers ----

    private PlayerSummaryResponse toSummary(Player p, UUID viewingTeamId) {
        return new PlayerSummaryResponse(
                p.getId(),
                p.getFirstName(),
                p.getLastName(),
                p.getPosition(),
                p.getHeightInches(),
                p.getAge(),
                p.isBenched(),
                p.isFreeAgent(),
                PlayerCostCalculator.calculateCostForTeam(p, viewingTeamId)
        );
    }

    private PlayerDetailResponse toDetail(Player p, UUID viewingTeamId) {
        Map<SkillType, Integer> skills = p.getSkills().stream()
                .collect(Collectors.toMap(PlayerSkill::getSkillType, PlayerSkill::getValue));

        return new PlayerDetailResponse(
                p.getId(),
                p.getFirstName(),
                p.getLastName(),
                p.getPosition(),
                p.getHeightInches(),
                p.getAge(),
                p.getPotential(),
                p.getPotentialRemaining(),
                p.getGrowth(),
                p.getLongevity(),
                p.getLongevityRemaining(),
                p.getDecay(),
                p.isBenched(),
                p.isFreeAgent(),
                p.getCurrentTeam() != null ? p.getCurrentTeam().getId() : null,
                p.getCurrentTeam() != null ? p.getCurrentTeam().getName() : null,
                p.getOriginalTeam() != null ? p.getOriginalTeam().getId() : null,
                p.getOriginalTeam() != null ? p.getOriginalTeam().getName() : null,
                skills,
                PlayerCostCalculator.calculateCostForTeam(p, viewingTeamId)
        );
    }

    private PlayerSeasonStatsResponse toStatsResponse(PlayerSeasonStats s) {
        return new PlayerSeasonStatsResponse(
                s.getId(),
                s.getYearNumber(),
                s.getTeam() != null ? s.getTeam().getName() : null,
                s.getGamesPlayed(),
                s.getTwoPointAttempts(),
                s.getTwoPointMade(),
                s.getFourPointAttempts(),
                s.getFourPointMade(),
                s.getFreeThrowAttempts(),
                s.getFreeThrowMade(),
                s.getPassesAttempted(),
                s.getPassesCompleted(),
                s.getStealsAttempted(),
                s.getStealsMade(),
                s.getBlocksAttempted(),
                s.getBlocksMade(),
                s.getReboundsAttempted(),
                s.getReboundsMade(),
                s.getTotalPoints(),
                s.getPossessionWins(),
                s.getPossessionLosses()
        );
    }

    private void validateScenarioOwnership(String username, UUID scenarioId) {
        accountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Scenario not found"));
    }

    private UUID getUserTeamId(UUID scenarioId) {
        return teamRepository.findByScenarioIdAndUserTeamTrue(scenarioId)
                .map(Team::getId)
                .orElse(null);
    }
}

