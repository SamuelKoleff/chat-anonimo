package dev.skoleff.chat_service;

import dev.skoleff.common_events.RoomCreatedEvent;
import dev.skoleff.common_events.UserMatchedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MatchListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public MatchListener(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "user.matched", groupId = "chat-service")
    public void onUserMatched(UserMatchedEvent event) {
        String matchId = UUID.randomUUID().toString();

        String room = "/topic/match/" + matchId;

        kafkaTemplate.send("room.created", new RoomCreatedEvent(event.sessionId1(), event.sessionId2(), matchId));
    }
}
