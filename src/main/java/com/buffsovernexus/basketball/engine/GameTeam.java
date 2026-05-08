package com.buffsovernexus.basketball.engine;

import com.buffsovernexus.basketball.entity.Team;

/**
 * In-memory snapshot of a team during a game.
 * Holds the two active players (Guard + Forward) and live score.
 */
public class GameTeam {

    private final Team entity;
    private final GamePlayer guard;
    private final GamePlayer forward;
    private int score;

    public GameTeam(Team entity, GamePlayer guard, GamePlayer forward) {
        this.entity = entity;
        this.guard = guard;
        this.forward = forward;
        this.score = 0;
    }

    public void addPoints(int pts) { score += pts; }

    public Team getEntity() { return entity; }
    public String getName() { return entity.getName(); }
    public GamePlayer getGuard() { return guard; }
    public GamePlayer getForward() { return forward; }
    public int getScore() { return score; }
}

