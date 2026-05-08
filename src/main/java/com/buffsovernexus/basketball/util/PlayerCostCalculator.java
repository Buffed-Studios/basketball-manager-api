package com.buffsovernexus.basketball.util;

import com.buffsovernexus.basketball.entity.Player;
import com.buffsovernexus.basketball.entity.PlayerSkill;

public class PlayerCostCalculator {

    private static final long BASE_COST = 1_000_000L;
    private static final double LOYALTY_DISCOUNT = 0.80; // 20% discount for original team

    /**
     * Calculates the cost of a player based on their lifecycle stage and stats.
     *
     * Lifecycle stages:
     *   - Developing: potentialRemaining > 0
     *   - Prime:      potentialRemaining == 0 && longevityRemaining > 0
     *   - Declining:  potentialRemaining == 0 && longevityRemaining == 0
     */
    public static long calculateCost(Player player) {
        int totalSkill = player.getSkills().stream().mapToInt(PlayerSkill::getValue).sum();

        double stageMult;
        if (player.getPotentialRemaining() > 0) {
            // Developing — valued on future potential, cheaper now
            double futureGrowth = (double) player.getPotentialRemaining() * player.getGrowth();
            stageMult = 0.5 + (futureGrowth / 100.0);
        } else if (player.getLongevityRemaining() > 0) {
            // Prime — most expensive stage
            double primeBonus = (double) player.getLongevity() * player.getLongevityRemaining();
            stageMult = 1.5 + (primeBonus / 50.0);
        } else {
            // Declining — discounted based on decay
            stageMult = Math.max(0.1, 0.8 - (player.getDecay() / 100.0));
        }

        long cost = (long) (BASE_COST + (totalSkill * 150_000L * stageMult));
        return Math.max(BASE_COST, cost);
    }

    /**
     * Applies a loyalty discount if the requesting team is the player's original team.
     */
    public static long calculateCostForTeam(Player player, java.util.UUID teamId) {
        long baseCost = calculateCost(player);
        if (player.getOriginalTeam() != null && player.getOriginalTeam().getId().equals(teamId)) {
            return (long) (baseCost * LOYALTY_DISCOUNT);
        }
        return baseCost;
    }

    private PlayerCostCalculator() {}
}

