package com.versus.api.websocket;

import java.util.UUID;

public record MatchEventEnvelope<T>(String type, UUID matchId, T payload) {
    public static <T> MatchEventEnvelope<T> of(String type, UUID matchId, T payload) {
        return new MatchEventEnvelope<>(type, matchId, payload);
    }
}
