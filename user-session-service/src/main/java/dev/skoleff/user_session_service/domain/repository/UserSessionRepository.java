package dev.skoleff.user_session_service.domain.repository;


import dev.skoleff.user_session_service.domain.model.UserSession;

import java.util.Optional;

public interface UserSessionRepository {
    void save(UserSession session);
    Optional<UserSession> findById(String sessionId);
    void delete(String sessionId);
}
