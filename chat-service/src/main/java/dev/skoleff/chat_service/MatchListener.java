package dev.skoleff.chat_service;

import dev.skoleff.common_events.UserMatchedEvent;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class MatchListener {

    private final RedisTemplate<String, Object> redisTemplate;

    public MatchListener(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(topics = "user.matched", groupId = "chat-service")
    public void onUserMatched(UserMatchedEvent event) {
        String matchId = UUID.randomUUID().toString();

        String key = "match:" + matchId;
        redisTemplate.opsForHash().putAll(key, Map.of(
                "user1", event.user1(),
                "user2", event.user2(),
                "createdAt", Instant.now().toString()
        ));
        redisTemplate.expire(key, java.time.Duration.ofHours(1));

        String room = "/topic/match/" + matchId;

        System.out.println("💬 Nuevo chat creado para " + event.user1() + " y " + event.user2());
        System.out.println("➡️ Room: " + room);

        // Opcional: enviar un evento Kafka con la info del chat
        // kafkaTemplate.send("chat.room.created", new ChatRoomCreatedEvent(event.user1(), event.user2(), matchId, room));
    }
}
