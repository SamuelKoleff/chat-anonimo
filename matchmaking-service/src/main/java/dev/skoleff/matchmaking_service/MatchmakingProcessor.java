package dev.skoleff.matchmaking_service;

import dev.skoleff.common_events.UserAvailableEvent;
import dev.skoleff.common_events.UserDisconnectedEvent;
import dev.skoleff.common_events.UserMatchedEvent;
import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class MatchmakingProcessor {

    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StreamMessageListenerContainer<String, MapRecord<String, String, String>>  container;

    private static final String STREAM_KEY = "matchmaking:queue";
    private static final String SET_KEY = "matchmaking:waiting";
    private static final Duration MATCH_TTL = Duration.ofHours(1);

    public MatchmakingProcessor(
            StringRedisTemplate redisTemplate,
            KafkaTemplate<String, Object> kafkaTemplate,
            StreamMessageListenerContainer<String, MapRecord<String, String, String>> container
    ) {
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.container = container;
    }

    @PostConstruct
    public void init() {
        try {
            redisTemplate.opsForStream()
                    .createGroup(STREAM_KEY, ReadOffset.latest(), "matchmaking-group");
        } catch (Exception e) {
            //Ignore si ya existe el grupo
        }

        container.receive(
                Consumer.from("matchmaking-group", "worker-1"),
                StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()),
                message -> {
                    processMessage(message);
                });
    }

    private void processMessage(MapRecord<String, String, String> msg) {
        String sessionId = msg.getValue().get("sessionId");

        System.out.println("Nuevo usuario en cola: " + sessionId);

        redisTemplate.opsForSet().add(SET_KEY, sessionId);

        boolean matched = tryMatch();

        if (matched) {
            redisTemplate.opsForStream().acknowledge(STREAM_KEY, "matchmaking-group", msg.getId());
        } else {
            System.out.println("Sin match aún");
        }
    }


    @KafkaListener(topics = "user.available", groupId = "matchmaking")
    public void onUserAvailable(UserAvailableEvent event) {
        String sessionId = event.sessionId();
        System.out.println("Recibido user.available desde Kafka: " + sessionId);

        Map<String, String> data = Map.of("sessionId", sessionId);

        redisTemplate.opsForStream().add(STREAM_KEY, data);
        redisTemplate.opsForSet().add(SET_KEY, sessionId);

        System.out.println("Usuario " + sessionId + " agregado a la cola Redis");
    }

    private boolean tryMatch() {
        var waiting = redisTemplate.opsForSet().members(SET_KEY);
        if (waiting == null || waiting.size() < 2) return false;

        var it = waiting.iterator();
        String s1 = it.next();
        String s2 = it.next();

        if (createMatch(s1, s2)) {
            redisTemplate.opsForSet().remove(SET_KEY, s1, s2);
            return true;
        }

        return false;
    }

    private boolean createMatch(String session1, String session2) {
        try {
            String matchId = UUID.randomUUID().toString();
            String matchKey = "match:" + matchId;

            redisTemplate.opsForHash().putAll(matchKey, Map.of(
                    "sessionId1", session1,
                    "sessionId2", session2,
                    "createdAt", Instant.now().toString()
            ));

            redisTemplate.expire(matchKey, MATCH_TTL);
            kafkaTemplate.send("user.matched", new UserMatchedEvent(matchId, session1, session2));

            System.out.printf("Match creado: %s entre %s y %s%n", matchId, session1, session2);

            return true;
        } catch (Exception ex) {
            System.err.println("Fallo match: " + ex.getMessage());
            return false;
        }
    }


}
