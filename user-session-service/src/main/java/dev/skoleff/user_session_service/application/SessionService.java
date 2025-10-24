package dev.skoleff.user_session_service.application;

import dev.skoleff.UserAvailableEvent;
import dev.skoleff.user_session_service.domain.model.UserSession;
import dev.skoleff.user_session_service.domain.repository.UserSessionRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SessionService {

    private final UserSessionRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SessionService(UserSessionRepository repository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void createSession(UserSession session) {
        session.setCreatedAt(Instant.now().toString());
        session.setLastPing(Instant.now().toString());
        repository.save(session);
    }

    public UserSession getSession(String sessionId) {
        return repository.findById(sessionId).orElse(null);
    }


    public void setAvailable(String sessionId){
        UserSession session = getSession(sessionId);
        if(session != null){
            session.setStatus("AVAILABLE");
            repository.save(session);
            kafkaTemplate.send("user.available", new UserAvailableEvent(sessionId));
            System.out.println("setting " + sessionId + " available and sending event");
        }
    }

    public void setMatched(String sessionId){
        UserSession session = getSession(sessionId);
        if(session != null){
            session.setStatus("MATCHED");
            repository.save(session);
        }
    }

    public void setDisconnected(String sessionId){
        UserSession session = getSession(sessionId);
        if(session != null){
            session.setStatus("DISCONNECTED");
            repository.save(session);
        }
    }

    public void deleteSession(String sessionId) {
        repository.delete(sessionId);
    }
}
