package com.buffsovernexus.basketball.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "player_season_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerSeasonStats {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "year_number", nullable = false)
    private int yearNumber;

    @Column(name = "games_played", nullable = false)
    private int gamesPlayed;

    @Column(name = "two_point_attempts", nullable = false)
    private int twoPointAttempts;

    @Column(name = "two_point_made", nullable = false)
    private int twoPointMade;

    @Column(name = "four_point_attempts", nullable = false)
    private int fourPointAttempts;

    @Column(name = "four_point_made", nullable = false)
    private int fourPointMade;

    @Column(name = "free_throw_attempts", nullable = false)
    private int freeThrowAttempts;

    @Column(name = "free_throw_made", nullable = false)
    private int freeThrowMade;

    @Column(name = "passes_attempted", nullable = false)
    private int passesAttempted;

    @Column(name = "passes_completed", nullable = false)
    private int passesCompleted;

    @Column(name = "steals_attempted", nullable = false)
    private int stealsAttempted;

    @Column(name = "steals_made", nullable = false)
    private int stealsMade;

    @Column(name = "blocks_attempted", nullable = false)
    private int blocksAttempted;

    @Column(name = "blocks_made", nullable = false)
    private int blocksMade;

    @Column(name = "rebounds_attempted", nullable = false)
    private int reboundsAttempted;

    @Column(name = "rebounds_made", nullable = false)
    private int reboundsMade;

    @Column(name = "total_points", nullable = false)
    private int totalPoints;

    @Column(name = "possession_wins", nullable = false)
    private int possessionWins;

    @Column(name = "possession_losses", nullable = false)
    private int possessionLosses;
}

