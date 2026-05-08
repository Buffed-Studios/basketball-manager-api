package com.buffsovernexus.basketball.engine;

import com.buffsovernexus.basketball.entity.SkillType;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.UUID;

/**
 * Core game engine. Accepts two GameTeams, runs the full game loop,
 * and broadcasts every event via WebSocket.
 *
 * Rules:
 *   - Home team has first possession.
 *   - First team to 20+ points wins.
 *   - When either team first reaches 10+, halftime is called (brief pause + stat snapshot).
 *   - Max 3 passes per possession before forced turnover.
 *   - 2 active players per team: Guard + Forward.
 *   - Guard shoots, Forward rebounds (and vice versa based on who has the ball).
 */
public class GameEngine {

    private static final int WIN_SCORE     = 20;
    private static final int HALFTIME_SCORE = 10;
    private static final int MAX_PASSES    = 3;
    private static final long EVENT_DELAY_MS      = 2_000;
    private static final long HALFTIME_DELAY_MS   = 10_000;

    private final UUID gameId;
    private final GameTeam home;
    private final GameTeam away;
    private final SimpMessagingTemplate messaging;
    private final String topic;
    /** When true, skips all sleep() delays and WebSocket broadcasts (instant simulation). */
    private final boolean fastMode;

    private boolean halftimeDone = false;
    /** Flips each possession so guard and forward alternate starting with the ball. */
    private boolean forwardStartsNext = false;
    /**
     * When set to true by an external caller (via {@link #triggerFastForward()}),
     * all sleep delays and play-by-play broadcasts are suppressed so the engine
     * races to the final whistle. A GAME_END event is still delivered.
     */
    private volatile boolean fastForward = false;

    /** Signal this engine to finish instantly. Safe to call from any thread. */
    public void triggerFastForward() {
        this.fastForward = true;
    }

    public GameEngine(UUID gameId,
                      GameTeam home,
                      GameTeam away,
                      SimpMessagingTemplate messaging,
                      String topic) {
        this(gameId, home, away, messaging, topic, false);
    }

    public GameEngine(UUID gameId,
                      GameTeam home,
                      GameTeam away,
                      SimpMessagingTemplate messaging,
                      String topic,
                      boolean fastMode) {
        this.gameId    = gameId;
        this.home      = home;
        this.away      = away;
        this.messaging = messaging;
        this.topic     = topic;
        this.fastMode  = fastMode;
    }

    // -------------------------------------------------------------------------
    // Public entry point
    // -------------------------------------------------------------------------

    public GameResult run() {
        broadcast(event(GameEventType.GAME_START,
                home.getName() + " vs " + away.getName() + " — Tip off!"));

        GameTeam offense = home;
        GameTeam defense = away;

        while (home.getScore() < WIN_SCORE && away.getScore() < WIN_SCORE) {
            checkHalftime();

            // Reset pass counts at start of each possession
            offense.getGuard().resetPassCount();
            offense.getForward().resetPassCount();

            // Run the possession — returns who gets the ball next
            GameTeam nextOffense = runPossession(offense, defense);

            if (nextOffense == offense) {
                // Offensive rebound — same team keeps ball
            } else {
                // Possession changes
                broadcast(event(GameEventType.POSSESSION_CHANGE,
                        nextOffense.getName() + " gains possession.")
                        .with("team", nextOffense.getName()));
                GameTeam tmp = offense;
                offense = nextOffense;
                defense = tmp;
            }
        }

        GameTeam winner = home.getScore() >= WIN_SCORE ? home : away;
        // Always deliver the final result — even if the game was fast-forwarded,
        // the UI needs the GAME_END event to show the final score.
        if (!fastMode && topic != null) {
            messaging.convertAndSend(topic, event(GameEventType.GAME_END,
                    winner.getName() + " wins " + home.getScore() + " - " + away.getScore() + "!")
                    .with("winner", winner.getName())
                    .with("homeScore", home.getScore())
                    .with("awayScore", away.getScore())
                    .with("fastForwarded", fastForward));
        }

        return buildResult(winner);
    }

    // -------------------------------------------------------------------------
    // Halftime
    // -------------------------------------------------------------------------

    private void checkHalftime() {
        if (!halftimeDone && (home.getScore() >= HALFTIME_SCORE || away.getScore() >= HALFTIME_SCORE)) {
            halftimeDone = true;

            GameEvent ht = event(GameEventType.HALFTIME,
                    "Halftime! " + home.getName() + " " + home.getScore()
                    + " - " + away.getScore() + " " + away.getName());
            broadcast(ht);

            // Broadcast stat snapshots for all 4 players
            broadcastStatSnapshot(home.getGuard());
            broadcastStatSnapshot(home.getForward());
            broadcastStatSnapshot(away.getGuard());
            broadcastStatSnapshot(away.getForward());

            sleep(HALFTIME_DELAY_MS);
        }
    }

    private void broadcastStatSnapshot(GamePlayer p) {
        int twoAtt  = p.getTwoPointAttempts();
        int fourAtt = p.getFourPointAttempts();
        double shotAcc = (twoAtt + fourAtt) == 0 ? 0.0
                : (double)(p.getTwoPointMade() + p.getFourPointMade()) / (twoAtt + fourAtt) * 100;
        double rebAcc = p.getReboundsAttempted() == 0 ? 0.0
                : (double) p.getReboundsMade() / p.getReboundsAttempted() * 100;
        double passAcc = p.getPassesAttempted() == 0 ? 0.0
                : (double) p.getPassesCompleted() / p.getPassesAttempted() * 100;

        broadcast(event(GameEventType.PLAYER_STATS_SNAPSHOT, p.getFullName() + " halftime stats")
                .with("player", p.getFullName())
                .with("points", p.getTotalPoints())
                .with("shotAccuracy", String.format("%.1f%%", shotAcc))
                .with("reboundAccuracy", String.format("%.1f%%", rebAcc))
                .with("passAccuracy", String.format("%.1f%%", passAcc))
                .with("steals", p.getStealsMade())
                .with("blocks", p.getBlocksMade()));
    }

    // -------------------------------------------------------------------------
    // Possession loop
    // -------------------------------------------------------------------------

    /**
     * Runs a single possession. Returns the team that should next have the ball.
     * Same team = offensive rebound. Other team = turnover/miss/steal/made basket.
     */
    private GameTeam runPossession(GameTeam offense, GameTeam defense) {
        // Alternate who starts with the ball: guard one possession, forward the next.
        GamePlayer ballHandler;
        GamePlayer offTeammate;
        if (forwardStartsNext) {
            ballHandler = offense.getForward();
            offTeammate  = offense.getGuard();
        } else {
            ballHandler = offense.getGuard();
            offTeammate  = offense.getForward();
        }
        forwardStartsNext = !forwardStartsNext;

        int passCount = 0;

        while (true) {
            // --- DECISION: Shoot or Pass? ---
            boolean shouldShoot = (passCount >= MAX_PASSES)
                    || GameCalculator.decideShouldShoot(ballHandler, offTeammate, passCount);

            if (shouldShoot) {
                return resolveShot(offense, defense, ballHandler, offTeammate);
            } else {
                // --- PASS ---
                passCount++;
                ballHandler.incrementPassCount();

                GamePlayer defPressure = matchupDefender(ballHandler, defense);
                ballHandler.addPassAttempted();

                broadcast(event(GameEventType.PASS_ATTEMPTED,
                        ballHandler.getFullName() + " attempts a pass to " + offTeammate.getFullName() + ".")
                        .with("passer", ballHandler.getFullName())
                        .with("target", offTeammate.getFullName()));
                sleep(EVENT_DELAY_MS);

                // Steal attempt?
                defPressure.addStealAttempted();

                if (GameCalculator.resolveSteal(defPressure, ballHandler)) {
                    // Check foul on steal first
                    if (GameCalculator.resolveFoulOnSteal(defPressure)) {
                        return resolveFoulEvent(offense, defense, ballHandler, 1, "steal");
                    }

                    // Steal succeeds
                    defPressure.addStealMade();
                    ballHandler.addPossessionLoss();
                    defPressure.addPossessionWin();

                    broadcast(event(GameEventType.PASS_STOLEN,
                            defPressure.getFullName() + " steals the pass!")
                            .with("stealer", defPressure.getFullName())
                            .with("passer", ballHandler.getFullName()));
                    sleep(EVENT_DELAY_MS);
                    return defense;
                }

                // Pass success check
                if (!GameCalculator.resolvePassSuccess(ballHandler)) {
                    // Bad pass — OOB or turnover
                    ballHandler.addPossessionLoss();
                    if (GameCalculator.resolvePassOutOfBounds()) {
                        broadcast(event(GameEventType.PASS_OUT_OF_BOUNDS,
                                ballHandler.getFullName() + "'s pass went out of bounds!")
                                .with("passer", ballHandler.getFullName()));
                    } else {
                        broadcast(event(GameEventType.PASS_TURNOVER,
                                ballHandler.getFullName() + " threw a bad pass — turnover!")
                                .with("passer", ballHandler.getFullName()));
                    }
                    sleep(EVENT_DELAY_MS);
                    defense.getGuard().addPossessionWin();
                    return defense;
                }

                // Pass completed — swap ball handler
                ballHandler.addPassCompleted();
                broadcast(event(GameEventType.PASS_COMPLETED,
                        ballHandler.getFullName() + " passes to " + offTeammate.getFullName() + ".")
                        .with("passer", ballHandler.getFullName())
                        .with("receiver", offTeammate.getFullName()));
                sleep(EVENT_DELAY_MS);

                // Swap roles
                GamePlayer tmp = ballHandler;
                ballHandler = offTeammate;
                offTeammate = tmp;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Shot resolution
    // -------------------------------------------------------------------------

    private GameTeam resolveShot(GameTeam offense, GameTeam defense,
                                  GamePlayer shooter, GamePlayer offRebounder) {
        // Decide shot type: 4-pointer vs 2-pointer
        boolean attempting4 = GameCalculator.decideShoot4(shooter);
        SkillType shotSkill  = attempting4 ? SkillType.FOUR_POINT : SkillType.TWO_POINT;
        String shotLabel     = attempting4 ? "4-pointer" : "2-pointer";

        broadcast(event(GameEventType.SHOT_DECISION,
                shooter.getFullName() + " decides to attempt a " + shotLabel + "!")
                .with("shooter", shooter.getFullName())
                .with("shotType", shotLabel)
                .with("streakModifier", shooter.getStreakModifier()));
        sleep(EVENT_DELAY_MS);

        if (attempting4) shooter.addFourPointAttempt();
        else             shooter.addTwoPointAttempt();

        broadcast(event(GameEventType.SHOT_ATTEMPTED,
                shooter.getFullName() + " shoots a " + shotLabel + "!")
                .with("shooter", shooter.getFullName())
                .with("shotType", shotLabel));
        sleep(EVENT_DELAY_MS);

        // Block attempt by opposing player that matches position
        GamePlayer blocker = matchupDefender(shooter, defense);
        blocker.addBlockAttempted();

        if (GameCalculator.resolveBlock(blocker, shooter, shotSkill)) {
            // Check foul on block
            if (GameCalculator.resolveFoulOnBlock(blocker)) {
                return resolveFoulEvent(offense, defense, shooter,
                        attempting4 ? 2 : 1, "shot");
            }

            blocker.addBlockMade();
            shooter.recordShotResult(false);
            broadcastStreakEvent(shooter);

            broadcast(event(GameEventType.SHOT_BLOCKED,
                    blocker.getFullName() + " blocks the " + shotLabel + "!")
                    .with("blocker", blocker.getFullName())
                    .with("shooter", shooter.getFullName()));
            sleep(EVENT_DELAY_MS);

            // Blocked shot — rebound contest
            return resolveRebound(offense, defense, offRebounder,
                    matchupDefender(offRebounder, defense));
        }

        // Shot result
        boolean made = attempting4
                ? GameCalculator.resolveShot4(shooter)
                : GameCalculator.resolveShot2(shooter);

        shooter.recordShotResult(made);
        broadcastStreakEvent(shooter);

        if (made) {
            int pts = attempting4 ? 4 : 2;
            if (attempting4) shooter.addFourPointMade();
            else             shooter.addTwoPointMade();
            shooter.addPossessionWin();

            offense.addPoints(pts);

            broadcast(event(GameEventType.SHOT_MADE,
                    shooter.getFullName() + " scores " + pts + " points!")
                    .with("shooter", shooter.getFullName())
                    .with("points", pts)
                    .with("homeScore", home.getScore())
                    .with("awayScore", away.getScore()));
            sleep(EVENT_DELAY_MS);

            // Check win immediately
            if (offense.getScore() >= WIN_SCORE) {
                return offense; // engine loop will catch it
            }

            // And-one chance
            if (GameCalculator.resolveFoulOnBlock(blocker)) {
                broadcast(event(GameEventType.FOUL,
                        "And-one foul on " + blocker.getFullName() + "!")
                        .with("fouled", shooter.getFullName())
                        .with("fouler", blocker.getFullName()));
                sleep(EVENT_DELAY_MS);
                resolveAndOneFreeThrow(offense, defense, shooter);
            }

            return defense; // made basket — possession changes
        }

        // Shot missed
        broadcast(event(GameEventType.SHOT_MISSED,
                shooter.getFullName() + "'s " + shotLabel + " is off the mark.")
                .with("shooter", shooter.getFullName()));
        sleep(EVENT_DELAY_MS);

        if (GameCalculator.resolveOutOfBoundsOnMiss()) {
            shooter.addPossessionLoss();
            broadcast(event(GameEventType.SHOT_OUT_OF_BOUNDS,
                    "The ball goes out of bounds off " + shooter.getFullName() + "'s miss!")
                    .with("shooter", shooter.getFullName()));
            sleep(EVENT_DELAY_MS);
            return defense;
        }

        // Rebound
        return resolveRebound(offense, defense, offRebounder,
                matchupDefender(offRebounder, defense));
    }

    // -------------------------------------------------------------------------
    // Rebound resolution
    // -------------------------------------------------------------------------

    private GameTeam resolveRebound(GameTeam offense, GameTeam defense,
                                     GamePlayer offRebounder, GamePlayer defRebounder) {
        offRebounder.addReboundAttempted();
        defRebounder.addReboundAttempted();

        broadcast(event(GameEventType.POSSESSION_CHANGE,
                offRebounder.getFullName() + " and " + defRebounder.getFullName()
                + " battle for the rebound.")
                .with("offRebounder", offRebounder.getFullName())
                .with("defRebounder", defRebounder.getFullName()));
        sleep(EVENT_DELAY_MS);

        // Foul during rebound?
        if (GameCalculator.resolveFoulOnRebound()) {
            broadcast(event(GameEventType.REBOUND_FOUL,
                    "Foul called during the rebound! " + offRebounder.getFullName()
                    + " goes to the line for 1 free throw.")
                    .with("fouled", offRebounder.getFullName()));
            sleep(EVENT_DELAY_MS);
            resolveFreeThrows(offense, defense, offRebounder, 1);
            return defense; // after rebound foul, defense gets possession
        }

        // Contested out of bounds?
        if (GameCalculator.resolveReboundOutOfBounds()) {
            broadcast(event(GameEventType.REBOUND_OUT_OF_BOUNDS,
                    "The rebound goes out of bounds! Defense ball.")
                    .with("offRebounder", offRebounder.getFullName())
                    .with("defRebounder", defRebounder.getFullName()));
            sleep(EVENT_DELAY_MS);
            defRebounder.addPossessionWin();
            offRebounder.addPossessionLoss();
            return defense;
        }

        boolean offensiveRebound = GameCalculator.resolveOffensiveRebound(offRebounder, defRebounder);

        if (offensiveRebound) {
            offRebounder.addReboundMade();
            offRebounder.addPossessionWin();
            broadcast(event(GameEventType.REBOUND_OFFENSIVE,
                    offRebounder.getFullName() + " grabs the offensive rebound!")
                    .with("rebounder", offRebounder.getFullName()));
            sleep(EVENT_DELAY_MS);
            return offense;
        } else {
            defRebounder.addReboundMade();
            defRebounder.addPossessionWin();
            offRebounder.addPossessionLoss();
            broadcast(event(GameEventType.REBOUND_DEFENSIVE,
                    defRebounder.getFullName() + " secures the defensive rebound!")
                    .with("rebounder", defRebounder.getFullName()));
            sleep(EVENT_DELAY_MS);
            return defense;
        }
    }

    // -------------------------------------------------------------------------
    // Foul & Free Throw resolution
    // -------------------------------------------------------------------------

    /**
     * Handles a foul mid-action and sends fouled player to the line.
     * Context: "steal", "shot"
     */
    private GameTeam resolveFoulEvent(GameTeam offense, GameTeam defense,
                                       GamePlayer fouledPlayer, int freeThrows,
                                       String context) {
        String foulerName = matchupDefender(fouledPlayer, defense).getFullName();
        broadcast(event(GameEventType.FOUL,
                "Foul during " + context + "! " + fouledPlayer.getFullName()
                + " goes to the line for " + freeThrows + " free throw(s).")
                .with("fouled", fouledPlayer.getFullName())
                .with("fouler", foulerName)
                .with("freeThrows", freeThrows));
        sleep(EVENT_DELAY_MS);

        resolveFreeThrows(offense, defense, fouledPlayer, freeThrows);
        return defense; // after foul FTs, defense gets possession
    }

    private void resolveFreeThrows(GameTeam offense, GameTeam defense,
                                    GamePlayer shooter, int count) {
        for (int i = 1; i <= count; i++) {
            shooter.addFreeThrowAttempt();
            broadcast(event(GameEventType.FREE_THROW_ATTEMPTED,
                    shooter.getFullName() + " at the line (" + i + "/" + count + ").")
                    .with("shooter", shooter.getFullName())
                    .with("attempt", i)
                    .with("total", count));
            sleep(EVENT_DELAY_MS);

            if (GameCalculator.resolveFreeThrow(shooter)) {
                shooter.addFreeThrowMade();
                offense.addPoints(1);
                broadcast(event(GameEventType.FREE_THROW_MADE,
                        shooter.getFullName() + " makes the free throw!")
                        .with("shooter", shooter.getFullName())
                        .with("homeScore", home.getScore())
                        .with("awayScore", away.getScore()));
            } else {
                broadcast(event(GameEventType.FREE_THROW_MISSED,
                        shooter.getFullName() + " misses the free throw.")
                        .with("shooter", shooter.getFullName()));
            }
            sleep(EVENT_DELAY_MS);

            // Check win after each free throw
            if (offense.getScore() >= WIN_SCORE) break;
        }
    }

    private void resolveAndOneFreeThrow(GameTeam offense, GameTeam defense, GamePlayer shooter) {
        resolveFreeThrows(offense, defense, shooter, 1);
    }

    // -------------------------------------------------------------------------
    // Streak events
    // -------------------------------------------------------------------------

    private void broadcastStreakEvent(GamePlayer player) {
        double mod = player.getStreakModifier();
        if (mod > 0) {
            broadcast(event(GameEventType.HOT_STREAK,
                    player.getFullName() + " is on fire! 🔥 Shooting boost active.")
                    .with("player", player.getFullName())
                    .with("modifier", mod));
        } else if (mod < 0) {
            broadcast(event(GameEventType.COLD_STREAK,
                    player.getFullName() + " is ice cold! ❄️ Shooting penalty active.")
                    .with("player", player.getFullName())
                    .with("modifier", mod));
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the defender that matches up against the given offensive player.
     * Guards match up against guards; forwards against forwards.
     */
    private GamePlayer matchupDefender(GamePlayer offPlayer, GameTeam defTeam) {
        boolean offIsGuard = offPlayer.getEntity().getPosition()
                == com.buffsovernexus.basketball.entity.Position.GUARD;
        return offIsGuard ? defTeam.getGuard() : defTeam.getForward();
    }

    private GameEvent event(GameEventType type, String description) {
        return new GameEvent(type, description, home.getScore(), away.getScore());
    }

    private void broadcast(GameEvent event) {
        if (fastMode || fastForward) return;
        if (topic != null) {
            messaging.convertAndSend(topic, event);
        }
    }

    private void sleep(long ms) {
        if (fastMode || fastForward) return;
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // -------------------------------------------------------------------------
    // Result builder
    // -------------------------------------------------------------------------

    private GameResult buildResult(GameTeam winner) {
        UUID homeTeamId = home.getEntity().getId();
        UUID awayTeamId = away.getEntity().getId();
        return new GameResult(
                gameId,
                home.getName(), home.getScore(),
                away.getName(), away.getScore(),
                winner.getName(),
                List.of(
                        buildPlayerStats(home.getGuard(),    homeTeamId),
                        buildPlayerStats(home.getForward(),  homeTeamId),
                        buildPlayerStats(away.getGuard(),    awayTeamId),
                        buildPlayerStats(away.getForward(),  awayTeamId)
                )
        );
    }

    private GameResult.PlayerStats buildPlayerStats(GamePlayer p, UUID teamId) {
        return new GameResult.PlayerStats(
                p.getEntity().getId(),
                teamId,
                p.getFullName(),
                p.getTwoPointAttempts(), p.getTwoPointMade(),
                p.getFourPointAttempts(), p.getFourPointMade(),
                p.getFreeThrowAttempts(), p.getFreeThrowMade(),
                p.getPassesAttempted(), p.getPassesCompleted(),
                p.getStealsAttempted(), p.getStealsMade(),
                p.getBlocksAttempted(), p.getBlocksMade(),
                p.getReboundsAttempted(), p.getReboundsMade(),
                p.getTotalPoints(),
                p.getPossessionWins(), p.getPossessionLosses()
        );
    }
}



