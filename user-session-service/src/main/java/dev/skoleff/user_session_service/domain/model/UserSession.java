package dev.skoleff.user_session_service.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserSession {
    private String sessionId;
    private String userId;
    private String status; // UP, AVAILABLE, MATCHED, DISCONNECTED
    private String roomId;
    private String createdAt;
    private String lastPing;

    public UserSession() {}

    public UserSession(String sessionId, String userId) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.status = "UP";
        this.roomId = null;
        this.createdAt = Instant.now().toString();
        this.lastPing = Instant.now().toString();
    }

    public void setStatusUp(){
        this.status = "UP";
    }

    public boolean isUp(){
        return this.status.equals("UP");
    }

    public boolean isMatched(){
        return this.status.equals("MATCHED");
    }

    public boolean isAvailable(){
        return this.status.equals("AVAILABLE");
    }

    public boolean isDisconnected(){
        return this.status.equals("DISCONNECTED");
    }

    public void setStatusMathed(){
        this.status = "MATCHED";
    }

    public void setStatusAvailable(){
        this.status = "AVAILABLE";
    }

    public void setStatusDisconnectd(){
        this.status = "DISCONNECTED";
    }


    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getStatus() { return status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getLastPing() { return lastPing; }
    public void setLastPing(String lastPing) { this.lastPing = lastPing; }
}