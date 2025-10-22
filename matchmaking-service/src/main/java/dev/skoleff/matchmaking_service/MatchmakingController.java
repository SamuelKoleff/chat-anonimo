package dev.skoleff.matchmaking_service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/matchmaking")
public class MatchmakingController {

    private final StringRedisTemplate redisTemplate;
    private static final String STREAM_KEY = "matchmaking:queue";

    public MatchmakingController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    @PostMapping("/join")
    public ResponseEntity<String> joinQueue(@RequestBody String sessionId) {

        Map<String, String> data = Map.of("sessionId", sessionId);

        redisTemplate.opsForStream().add(STREAM_KEY, data);

        redisTemplate.opsForSet().add("matchmaking:waiting", sessionId);

        return ResponseEntity.ok("Usuario agregado a la cola");
    }
}

