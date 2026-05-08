package com.buffsovernexus.basketball.engine;

import com.buffsovernexus.basketball.entity.SkillType;

import java.util.Random;

/**
 * All probability calculations for game actions.
 * Skills are rated 1-10. All methods return true = success.
 */
public class GameCalculator {

    private static final Random RNG = new Random();

    // -------------------------------------------------------------------------
    // SHOT
    // -------------------------------------------------------------------------

    /**
     * Determines if a 2-point shot is made.
     * Base chance scales with shooter's TWO_POINT skill.
     * Streak modifier applied on top.
     */
    public static boolean resolveShot2(GamePlayer shooter) {
        double base = 0.30 + (shooter.getSkill(SkillType.TWO_POINT) / 10.0) * 0.45;
        double chance = clamp(base + shooter.getStreakModifier());
        return roll(chance);
    }

    /**
     * Determines if a 4-point shot is made.
     * Harder base chance. Streak modifier applied.
     */
    public static boolean resolveShot4(GamePlayer shooter) {
        double base = 0.15 + (shooter.getSkill(SkillType.FOUR_POINT) / 10.0) * 0.35;
        double chance = clamp(base + shooter.getStreakModifier());
        return roll(chance);
    }

    /**
     * Determines if a missed shot went out of bounds instead of being rebounded.
     * ~15% of misses go out.
     */
    public static boolean resolveOutOfBoundsOnMiss() {
        return roll(0.15);
    }

    // -------------------------------------------------------------------------
    // BLOCK
    // -------------------------------------------------------------------------

    /**
     * Determines if a block attempt succeeds.
     * Defender's BLOCKING vs shooter's skill level.
     */
    public static boolean resolveBlock(GamePlayer defender, GamePlayer shooter, SkillType shotType) {
        double defSkill = defender.getSkill(SkillType.BLOCKING) / 10.0;
        double offSkill = shooter.getSkill(shotType) / 10.0;
        // Base lowered to 0.02; defender contribution reduced to 0.15 so even an elite
        // blocker tops out around ~17% block rate against a weak shooter.
        double chance = clamp(0.02 + (defSkill * 0.15) - (offSkill * 0.08));
        return roll(chance);
    }

    /**
     * Determines if a block attempt results in a foul instead.
     * Higher blocker skill = less likely to foul.
     */
    public static boolean resolveFoulOnBlock(GamePlayer defender) {
        double foulChance = clamp(0.25 - (defender.getSkill(SkillType.BLOCKING) / 10.0) * 0.15);
        return roll(foulChance);
    }

    // -------------------------------------------------------------------------
    // STEAL
    // -------------------------------------------------------------------------

    /**
     * Determines if a steal attempt succeeds on a pass.
     * Defender STEALING vs passer PASSING.
     */
    public static boolean resolveSteal(GamePlayer defender, GamePlayer passer) {
        double defSkill = defender.getSkill(SkillType.STEALING) / 10.0;
        double offSkill = passer.getSkill(SkillType.PASSING) / 10.0;
        double chance = clamp(0.08 + (defSkill * 0.25) - (offSkill * 0.12));
        return roll(chance);
    }

    /**
     * Determines if a steal attempt results in a foul.
     * Higher stealer skill = less likely to foul.
     */
    public static boolean resolveFoulOnSteal(GamePlayer defender) {
        double foulChance = clamp(0.20 - (defender.getSkill(SkillType.STEALING) / 10.0) * 0.10);
        return roll(foulChance);
    }

    /**
     * Determines if a failed pass goes out of bounds (vs just a bad pass turnover).
     * ~30% of failed passes go out.
     */
    public static boolean resolvePassOutOfBounds() {
        return roll(0.30);
    }

    // -------------------------------------------------------------------------
    // REBOUND
    // -------------------------------------------------------------------------

    /**
     * Advanced rebound check. Compares both rebounders using height + REBOUNDING skill
     * with weighted random. Returns true if the offensive player wins the rebound.
     *
     * Height contributes 40%, skill contributes 50%, pure randomness 10%.
     */
    public static boolean resolveOffensiveRebound(GamePlayer offensive, GamePlayer defensive) {
        double offScore = (offensive.getHeightInches() / 96.0) * 0.40
                + (offensive.getSkill(SkillType.REBOUNDING) / 10.0) * 0.50
                + RNG.nextDouble() * 0.10;

        double defScore = (defensive.getHeightInches() / 96.0) * 0.40
                + (defensive.getSkill(SkillType.REBOUNDING) / 10.0) * 0.50
                + RNG.nextDouble() * 0.10;

        return offScore > defScore;
    }

    /**
     * Determines if a contested rebound results in the ball going out of bounds.
     * ~12% chance when both players contest.
     */
    public static boolean resolveReboundOutOfBounds() {
        return roll(0.12);
    }

    /**
     * Determines if a foul occurs during a rebound battle.
     * ~10% base chance.
     */
    public static boolean resolveFoulOnRebound() {
        return roll(0.10);
    }

    // -------------------------------------------------------------------------
    // FREE THROW
    // -------------------------------------------------------------------------

    /**
     * Determines if a free throw is made.
     * Uses FREE_THROW skill.
     */
    public static boolean resolveFreeThrow(GamePlayer shooter) {
        double chance = clamp(0.40 + (shooter.getSkill(SkillType.FREE_THROW) / 10.0) * 0.45);
        return roll(chance);
    }

    // -------------------------------------------------------------------------
    // PASS
    // -------------------------------------------------------------------------

    /**
     * Determines if a pass is successfully completed (before steal check).
     * Higher PASSING = less bad passes.
     */
    public static boolean resolvePassSuccess(GamePlayer passer) {
        double chance = clamp(0.60 + (passer.getSkill(SkillType.PASSING) / 10.0) * 0.30);
        return roll(chance);
    }

    // -------------------------------------------------------------------------
    // SHOT DECISION
    // -------------------------------------------------------------------------

    /**
     * Decides whether to attempt a 4-point shot vs a 2-point shot.
     * Considers shooter's FOUR_POINT skill. Higher skill = more willing to go for 4.
     * Returns true if the shooter attempts a 4-pointer.
     */
    public static boolean decideShoot4(GamePlayer shooter) {
        double chance = 0.20 + (shooter.getSkill(SkillType.FOUR_POINT) / 10.0) * 0.40;
        return roll(clamp(chance));
    }

    /**
     * Decides whether the offensive team should shoot now or pass.
     * Weighs:
     *   - Shooter's relevant skill (2pt or 4pt)
     *   - Shooter's streak (hot = more likely to shoot)
     *   - Pass count (more passes = more pressure to shoot)
     *   - Forward availability for 4-pointer
     *
     * Returns true if the team should shoot.
     */
    public static boolean decideShouldShoot(GamePlayer ballHandler, GamePlayer teammate, int passCount) {
        // Base shoot willingness from ball handler's best skill
        double bestShotSkill = Math.max(
                ballHandler.getSkill(SkillType.TWO_POINT),
                ballHandler.getSkill(SkillType.FOUR_POINT)
        ) / 10.0;

        // Streak influence: hot streak pushes toward shooting
        double streakFactor = ballHandler.getStreakModifier() * 1.5; // amplify streak influence on decision

        // Pass count pressure: after 2 passes, strong urge to shoot
        double passPressure = passCount * 0.15;

        // Teammate quality: if teammate has a great 4pt skill, ball handler may pass
        double teammateInfluence = -(teammate.getSkill(SkillType.FOUR_POINT) / 10.0) * 0.10;

        double shootChance = clamp(0.35 + bestShotSkill * 0.30 + streakFactor + passPressure + teammateInfluence);
        return roll(shootChance);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static boolean roll(double chance) {
        return RNG.nextDouble() < chance;
    }

    private static double clamp(double val) {
        return Math.max(0.05, Math.min(0.95, val));
    }

    private GameCalculator() {}
}

