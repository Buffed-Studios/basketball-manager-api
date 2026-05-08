package com.buffsovernexus.basketball.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Position position;

    @Column(name = "height_inches", nullable = false)
    private int heightInches;

    @Column(nullable = false)
    private int age;

    @Column(nullable = false)
    private int potential;

    @Column(name = "potential_remaining", nullable = false)
    private int potentialRemaining;

    @Column(nullable = false)
    private int growth;

    @Column(nullable = false)
    private int longevity;

    @Column(name = "longevity_remaining", nullable = false)
    private int longevityRemaining;

    @Column(nullable = false)
    private int decay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_team_id")
    private Team currentTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_team_id")
    private Team originalTeam;

    @Column(name = "is_benched", nullable = false)
    private boolean benched;

    @Column(name = "is_free_agent", nullable = false)
    private boolean freeAgent;

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<PlayerSkill> skills = new ArrayList<>();
}

