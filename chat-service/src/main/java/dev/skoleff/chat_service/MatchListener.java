package dev.skoleff.chat_service;

import dev.skoleff.common_events.RoomCreatedEvent;
import dev.skoleff.common_events.UserMatchedEvent;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class MatchListener {

    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public MatchListener(RedisTemplate<String, Object> redisTemplate, KafkaTemplate<String, Object> kafkaTemplate) {
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "user.matched", groupId = "chat-service")
    public void onUserMatched(UserMatchedEvent event) {
        String matchId = UUID.randomUUID().toString();

        String key = "match:" + matchId;
        redisTemplate.opsForHash().putAll(key, Map.of(
                "session1", event.sessionId1(),
                "session2", event.sessionId2(),
                "createdAt", Instant.now().toString()
        ));
        redisTemplate.expire(key, java.time.Duration.ofHours(1));

        String room = "/topic/match/" + matchId;

        System.out.println("Nuevo chat creado para " + event.sessionId1() + " y " + event.sessionId2());
        System.out.println("Room: " + room);

        kafkaTemplate.send("room.created", new RoomCreatedEvent(event.sessionId1(), event.sessionId2(), matchId));
    }
}
