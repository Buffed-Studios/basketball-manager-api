package com.buffsovernexus.basketball.repository;

import com.buffsovernexus.basketball.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {
    List<Team> findAllByScenarioId(UUID scenarioId);
    Optional<Team> findByIdAndScenarioId(UUID id, UUID scenarioId);
    Optional<Team> findByScenarioIdAndUserTeamTrue(UUID scenarioId);
}

