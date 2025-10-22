package dev.skoleff.matchmaking_service;

import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MatchmakingProcessor {

    private final StringRedisTemplate redisTemplate;
    private static final String STREAM_KEY = "matchmaking:queue";
    private static final Duration MATCH_TTL = Duration.ofHours(1);
    private static final int STREAM_MAX_LENGTH = 1000;

    public MatchmakingProcessor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            redisTemplate.opsForStream()
                    .createGroup(STREAM_KEY, ReadOffset.latest(), "matchmaking-group");
        } catch (Exception e) {
            //Ignore si ya existe el grupo
        }
    }

    @Scheduled(fixedDelay = 2000)
    public void processMatches() {
        List<MapRecord<String, Object, Object>> pending = readPendingMessages();

        if (pending.size() < 2) {
            List<MapRecord<String, Object, Object>> newMessages = readNewMessages();
            pending.addAll(newMessages);
        }

        System.out.println("Total para matchear: " + pending.size());

        if (pending.size() < 2) return;

        for (int i = 0; i + 1 < pending.size(); i += 2) {
            createMatch(pending.get(i), pending.get(i + 1));

            redisTemplate.opsForStream().acknowledge(STREAM_KEY, "matchmaking-group",
                    pending.get(i).getId(), pending.get(i + 1).getId());

            redisTemplate.opsForSet().remove("matchmaking:waiting",
                    pending.get(i).getValue().get("sessionId"),
                    pending.get(i + 1).getValue().get("sessionId"));
        }

        redisTemplate.opsForStream().trim(STREAM_KEY, STREAM_MAX_LENGTH);
    }

    private List<MapRecord<String, Object, Object>> readPendingMessages() {
        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
                .read(Consumer.from("matchmaking-group", "worker-1"),
                        StreamReadOptions.empty().count(10),
                        StreamOffset.create(STREAM_KEY, ReadOffset.from("0")));
        return records != null ? records : List.of();
    }

    private List<MapRecord<String, Object, Object>> readNewMessages() {
        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
                .read(Consumer.from("matchmaking-group", "worker-1"),
                        StreamReadOptions.empty().count(10),
                        StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()));
        return records != null ? records : List.of();
    }

    private void createMatch(MapRecord<String, Object, Object> r1, MapRecord<String, Object, Object> r2) {
        String matchId = UUID.randomUUID().toString();
        String session1 = (String) r1.getValue().get("sessionId");
        String session2 = (String) r2.getValue().get("sessionId");

        String matchKey = "match:" + matchId;
        redisTemplate.opsForHash().putAll(matchKey, Map.of(
                "sessionId1", session1,
                "sessionId2", session2,
                "createdAt", Instant.now().toString()
        ));

        redisTemplate.expire(matchKey, MATCH_TTL);

        System.out.printf("Match creado: %s entre %s y %s%n", matchId, session1, session2);
    }
}
