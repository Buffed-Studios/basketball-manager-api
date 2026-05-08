package com.buffsovernexus.basketball.repository;

import com.buffsovernexus.basketball.entity.Player;
import com.buffsovernexus.basketball.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerRepository extends JpaRepository<Player, UUID> {
    List<Player> findAllByCurrentTeamId(UUID teamId);
    List<Player> findAllByScenarioIdAndFreeAgentTrue(UUID scenarioId);
    Optional<Player> findByIdAndScenarioId(UUID id, UUID scenarioId);
    long countByCurrentTeamIdAndPositionAndBenchedFalse(UUID teamId, Position position);
    long countByCurrentTeamIdAndBenchedTrue(UUID teamId);
}

