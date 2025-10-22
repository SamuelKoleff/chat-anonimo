package dev.skoleff.chat_service;


import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/matches")
public class MatchController {

    private final RedisTemplate<String, Object> redisTemplate;

    public MatchController(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostMapping
    public Map<String, String> createMatch(@RequestBody MatchRequest request) {
        String matchId = UUID.randomUUID().toString();

        String key = "match:" + matchId;
        redisTemplate.opsForHash().putAll(key, Map.of(
                "user1", request.getUser1(),
                "user2", request.getUser2(),
                "createdAt", Instant.now().toString()
        ));

        redisTemplate.expire(key, java.time.Duration.ofHours(1));

        return Map.of(
                "matchId", matchId,
                "room", "/topic/match/" + matchId
        );
    }
}

