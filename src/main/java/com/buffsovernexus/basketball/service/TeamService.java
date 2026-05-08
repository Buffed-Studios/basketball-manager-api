package com.buffsovernexus.basketball.service;

import com.buffsovernexus.basketball.dto.PlayerSummaryResponse;
import com.buffsovernexus.basketball.dto.TeamRosterResponse;
import com.buffsovernexus.basketball.dto.TeamSummaryResponse;
import com.buffsovernexus.basketball.entity.Player;
import com.buffsovernexus.basketball.entity.Team;
import com.buffsovernexus.basketball.exception.ResourceNotFoundException;
import com.buffsovernexus.basketball.repository.AccountRepository;
import com.buffsovernexus.basketball.repository.PlayerRepository;
import com.buffsovernexus.basketball.repository.ScenarioRepository;
import com.buffsovernexus.basketball.repository.TeamRepository;
import com.buffsovernexus.basketball.util.PlayerCostCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final ScenarioRepository scenarioRepository;
    private final AccountRepository accountRepository;
    private final PlayerRepository playerRepository;

    @Transactional(readOnly = true)
    public List<TeamSummaryResponse> getTeams(String username, UUID scenarioId) {
        validateScenarioOwnership(username, scenarioId);

        return teamRepository.findAllByScenarioId(scenarioId).stream()
                .map(t -> new TeamSummaryResponse(t.getId(), t.getName(), t.getBudget(), t.isUserTeam()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TeamRosterResponse getTeamRoster(String username, UUID scenarioId, UUID teamId) {
        validateScenarioOwnership(username, scenarioId);

        Team team = teamRepository.findByIdAndScenarioId(teamId, scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));

        List<Player> all = playerRepository.findAllByCurrentTeamId(teamId);

        List<PlayerSummaryResponse> starters = all.stream()
                .filter(p -> !p.isBenched())
                .map(p -> toSummary(p, teamId))
                .toList();

        List<PlayerSummaryResponse> bench = all.stream()
                .filter(Player::isBenched)
                .map(p -> toSummary(p, teamId))
                .toList();

        return new TeamRosterResponse(
                team.getId(),
                team.getName(),
                team.getBudget(),
                team.isUserTeam(),
                starters,
                bench
        );
    }

    private void validateScenarioOwnership(String username, UUID scenarioId) {
        accountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Scenario not found"));
    }

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
}

