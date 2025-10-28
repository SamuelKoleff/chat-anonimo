package dev.skoleff.user_session_service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserSession(
        String sessionId,
        String userId,
        Status status,
        String roomId,
        String createdAt,
        String lastPing
) {

    public static UserSession create(String sessionId, String userId) {
        var now = Instant.now().toString();
        return new UserSession(sessionId, userId, Status.UP, null, now, now);
    }

    private static String now() {
        return Instant.now().toString();
    }

    public boolean isUp() {
        return status == Status.UP;
    }

    public boolean isAvailable() {
        return status == Status.AVAILABLE;
    }

    public boolean isMatched() {
        return status == Status.MATCHED;
    }

    public boolean isDisconnected() {
        return status == Status.DISCONNECTED;
    }

    public UserSession withStatus(Status newStatus) {
        return new UserSession(sessionId, userId, newStatus, roomId, createdAt, now());
    }

    public UserSession withRoomId(String newRoomId) {
        return new UserSession(sessionId, userId, status, newRoomId, createdAt, now());
    }

    public UserSession withLastPingNow() {
        return new UserSession(sessionId, userId, status, roomId, createdAt, now());
    }
}
