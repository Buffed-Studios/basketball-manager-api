package com.buffsovernexus.basketball.engine;

import java.util.HashMap;
import java.util.Map;

/**
 * A single game event that is broadcast over WebSocket.
 * The payload map carries context-specific details per event type.
 */
public class GameEvent {

    private final GameEventType type;
    private final String description;
    private final Map<String, Object> payload;
    private final int homeScore;
    private final int awayScore;

    public GameEvent(GameEventType type, String description, int homeScore, int awayScore) {
        this.type = type;
        this.description = description;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.payload = new HashMap<>();
    }

    public GameEvent with(String key, Object value) {
        payload.put(key, value);
        return this;
    }

    public GameEventType getType() { return type; }
    public String getDescription() { return description; }
    public Map<String, Object> getPayload() { return payload; }
    public int getHomeScore() { return homeScore; }
    public int getAwayScore() { return awayScore; }
}

