package dev.skoleff.user_session_service.domain.model;

import java.time.Instant;

public class UserSession {
    private String sessionId;
    private String userId;
    private String status; // AVAILABLE, MATCHED, DISCONNECTED
    private String createdAt;
    private String lastPing;

    public UserSession() {}

    public UserSession(String sessionId, String userId, String status) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.status = status;
        this.createdAt = Instant.now().toString();
        this.lastPing = Instant.now().toString();
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getLastPing() { return lastPing; }
    public void setLastPing(String lastPing) { this.lastPing = lastPing; }
}