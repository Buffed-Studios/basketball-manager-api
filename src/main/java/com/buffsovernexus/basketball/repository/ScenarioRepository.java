package com.buffsovernexus.basketball.repository;

import com.buffsovernexus.basketball.entity.Scenario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScenarioRepository extends JpaRepository<Scenario, UUID> {
    List<Scenario> findAllByAccountId(UUID accountId);
    Optional<Scenario> findByIdAndAccountId(UUID id, UUID accountId);
    boolean existsByAccountIdAndName(UUID accountId, String name);
}

