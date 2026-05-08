package com.buffsovernexus.basketball.engine;

import com.buffsovernexus.basketball.entity.Player;
import com.buffsovernexus.basketball.entity.SkillType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Lightweight in-memory snapshot of a player used during a game.
 * Tracks live state: score history, shot count, possession stats.
 */
public class GamePlayer {

    private final Player entity;
    private final Map<SkillType, Integer> skills;

    // Recent shot results: true = made, false = missed (last 2 only)
    private final boolean[] recentShots = new boolean[2];
    private int recentShotCount = 0;

    // Per-possession pass count (reset each possession)
    private int passCountThisPossession = 0;

    // In-game stat accumulators
    private int twoPointAttempts;
    private int twoPointMade;
    private int fourPointAttempts;
    private int fourPointMade;
    private int freeThrowAttempts;
    private int freeThrowMade;
    private int passesAttempted;
    private int passesCompleted;
    private int stealsAttempted;
    private int stealsMade;
    private int blocksAttempted;
    private int blocksMade;
    private int reboundsAttempted;
    private int reboundsMade;
    private int totalPoints;
    private int possessionWins;
    private int possessionLosses;

    public GamePlayer(Player entity) {
        this.entity = entity;
        this.skills = new EnumMap<>(SkillType.class);
        entity.getSkills().forEach(s -> skills.put(s.getSkillType(), s.getValue()));
    }

    public int getSkill(SkillType type) {
        return skills.getOrDefault(type, 1);
    }

    /** Returns streak modifier: +0.08 hot, -0.08 cold, 0 neutral */
    public double getStreakModifier() {
        if (recentShotCount < 2) return 0.0;
        boolean hot = recentShots[0] && recentShots[1];
        boolean cold = !recentShots[0] && !recentShots[1];
        if (hot) return 0.08;
        if (cold) return -0.08;
        return 0.0;
    }

    public void recordShotResult(boolean made) {
        recentShots[0] = recentShots[1];
        recentShots[1] = made;
        recentShotCount = Math.min(recentShotCount + 1, 2);
    }

    public void resetPassCount() { passCountThisPossession = 0; }
    public int getPassCount() { return passCountThisPossession; }
    public void incrementPassCount() { passCountThisPossession++; }

    // --- Stat mutators ---
    public void addTwoPointAttempt() { twoPointAttempts++; }
    public void addTwoPointMade() { twoPointMade++; totalPoints += 2; }
    public void addFourPointAttempt() { fourPointAttempts++; }
    public void addFourPointMade() { fourPointMade++; totalPoints += 4; }
    public void addFreeThrowAttempt() { freeThrowAttempts++; }
    public void addFreeThrowMade() { freeThrowMade++; totalPoints++; }
    public void addPassAttempted() { passesAttempted++; }
    public void addPassCompleted() { passesCompleted++; }
    public void addStealAttempted() { stealsAttempted++; }
    public void addStealMade() { stealsMade++; }
    public void addBlockAttempted() { blocksAttempted++; }
    public void addBlockMade() { blocksMade++; }
    public void addReboundAttempted() { reboundsAttempted++; }
    public void addReboundMade() { reboundsMade++; }
    public void addPossessionWin() { possessionWins++; }
    public void addPossessionLoss() { possessionLosses++; }

    // --- Getters ---
    public Player getEntity() { return entity; }
    public String getFullName() { return entity.getFirstName() + " " + entity.getLastName(); }
    public int getHeightInches() { return entity.getHeightInches(); }

    public int getTwoPointAttempts() { return twoPointAttempts; }
    public int getTwoPointMade() { return twoPointMade; }
    public int getFourPointAttempts() { return fourPointAttempts; }
    public int getFourPointMade() { return fourPointMade; }
    public int getFreeThrowAttempts() { return freeThrowAttempts; }
    public int getFreeThrowMade() { return freeThrowMade; }
    public int getPassesAttempted() { return passesAttempted; }
    public int getPassesCompleted() { return passesCompleted; }
    public int getStealsAttempted() { return stealsAttempted; }
    public int getStealsMade() { return stealsMade; }
    public int getBlocksAttempted() { return blocksAttempted; }
    public int getBlocksMade() { return blocksMade; }
    public int getReboundsAttempted() { return reboundsAttempted; }
    public int getReboundsMade() { return reboundsMade; }
    public int getTotalPoints() { return totalPoints; }
    public int getPossessionWins() { return possessionWins; }
    public int getPossessionLosses() { return possessionLosses; }
}

