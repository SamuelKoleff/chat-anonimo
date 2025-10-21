package dev.skoleff.user_session_service.application;

import dev.skoleff.user_session_service.domain.model.UserSession;
import dev.skoleff.user_session_service.domain.repository.UserSessionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SessionService {

    private final UserSessionRepository repository;

    public SessionService(UserSessionRepository repository) {
        this.repository = repository;
    }

    public void createSession(UserSession session) {
        session.setCreatedAt(Instant.now().toString());
        session.setLastPing(Instant.now().toString());
        repository.save(session);
    }

    public UserSession getSession(String sessionId) {
        return repository.findById(sessionId).orElse(null);
    }

    public void setStatus(String sessionId, String status) {
        UserSession s = getSession(sessionId);
        if (s != null) {
            s.setStatus(status);
            repository.save(s);
        }
    }

    public void deleteSession(String sessionId) {
        repository.delete(sessionId);
    }
}
