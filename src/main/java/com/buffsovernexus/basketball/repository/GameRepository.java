package com.buffsovernexus.basketball.repository;

import com.buffsovernexus.basketball.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GameRepository extends JpaRepository<Game, UUID> {
    List<Game> findAllByScenarioIdAndYearNumberOrderByGameNumber(UUID scenarioId, int yearNumber);
    List<Game> findAllByScenarioIdAndYearNumberAndPlayedFalseOrderByGameNumber(UUID scenarioId, int yearNumber);
    long countByScenarioIdAndYearNumber(UUID scenarioId, int yearNumber);
}

