package com.buffsovernexus.basketball.engine;

/**
 * All possible game event types broadcast over WebSocket.
 */
public enum GameEventType {
    // Game flow
    GAME_START,
    GAME_END,
    HALFTIME,
    POSSESSION_CHANGE,

    // Offense decisions
    SHOT_ATTEMPTED,
    SHOT_MADE,
    SHOT_MISSED,
    SHOT_BLOCKED,
    SHOT_OUT_OF_BOUNDS,

    // Pass events
    PASS_ATTEMPTED,
    PASS_COMPLETED,
    PASS_TURNOVER,       // bad pass, no steal credited
    PASS_STOLEN,         // defender gets the steal
    PASS_OUT_OF_BOUNDS,  // pass went out

    // Fouls & free throws
    FOUL,
    FREE_THROW_ATTEMPTED,
    FREE_THROW_MADE,
    FREE_THROW_MISSED,

    // Rebounds
    REBOUND_OFFENSIVE,
    REBOUND_DEFENSIVE,
    REBOUND_OUT_OF_BOUNDS,
    REBOUND_FOUL,

    // Streaks
    HOT_STREAK,
    COLD_STREAK,

    // Halftime player stats snapshot
    PLAYER_STATS_SNAPSHOT,

    // Shot decision
    SHOT_DECISION
}

