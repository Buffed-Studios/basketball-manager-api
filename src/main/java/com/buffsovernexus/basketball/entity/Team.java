package com.buffsovernexus.basketball.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private long budget;

    @Column(name = "is_user_team", nullable = false)
    private boolean userTeam;

    @OneToMany(mappedBy = "currentTeam", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Player> players = new ArrayList<>();
}

