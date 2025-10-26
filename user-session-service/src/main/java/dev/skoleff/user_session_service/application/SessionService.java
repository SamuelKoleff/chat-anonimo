package dev.skoleff.user_session_service.application;

import dev.skoleff.common_events.RoomCreatedEvent;
import dev.skoleff.common_events.UserAvailableEvent;
import dev.skoleff.common_events.UserMatchedEvent;
import dev.skoleff.user_session_service.domain.model.UserSession;
import dev.skoleff.user_session_service.domain.repository.UserSessionRepository;
import org.springframework.kafka.annotation.KafkaListener;
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
        session.setStatus("UP");
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

    public void setRoomId(String sessionId,String roomId){
        UserSession session = getSession(sessionId);
        if(session != null && roomId != null){
            session.setRoomId(roomId);
            repository.save(session);
        }
    }

    public void clearRoomId(String sessionId){
        UserSession session = getSession(sessionId);
        if(session != null){
            session.setRoomId(null);
            repository.save(session);
        }
    }

    @KafkaListener(topics = "room.created", groupId = "user-session-service")
    public void onRoomCreated(RoomCreatedEvent event){
        UserSession userSession1 = getSession(event.user1());
        UserSession userSession2 = getSession(event.user2());

        if(userSession1  != null && userSession2 != null && event.roomId() != null){
            userSession1.setRoomId(event.roomId());
            userSession2.setRoomId(event.roomId());
            repository.save(userSession1);
            repository.save(userSession2);
            System.out.println("room.created event: \n" + userSession1.toString() + "\n" + userSession2.toString());
        }
    }

    @KafkaListener(topics = "user.matched", groupId = "user-session-service")
    public void onUserMatched(UserMatchedEvent event){
        UserSession userSession1 = getSession(event.user1());
        UserSession userSession2 = getSession(event.user2());

        if(userSession1  != null && userSession2 != null){
            setMatched(userSession1.getSessionId());
            setMatched(userSession2.getSessionId());
            System.out.println("user.matched event: \n" + userSession1.toString() + "\n" + userSession2.toString());
        }
    }


    public void deleteSession(String sessionId) {
        repository.delete(sessionId);
    }
}
