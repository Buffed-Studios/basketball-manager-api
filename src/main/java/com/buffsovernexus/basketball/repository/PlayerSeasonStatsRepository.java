package com.buffsovernexus.basketball.repository;

import com.buffsovernexus.basketball.entity.PlayerSeasonStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerSeasonStatsRepository extends JpaRepository<PlayerSeasonStats, UUID> {
    List<PlayerSeasonStats> findAllByPlayerId(UUID playerId);
    List<PlayerSeasonStats> findAllByPlayerIdAndScenarioId(UUID playerId, UUID scenarioId);
    Optional<PlayerSeasonStats> findByPlayerIdAndScenarioIdAndYearNumberAndTeamId(
            UUID playerId, UUID scenarioId, int yearNumber, UUID teamId);
}

