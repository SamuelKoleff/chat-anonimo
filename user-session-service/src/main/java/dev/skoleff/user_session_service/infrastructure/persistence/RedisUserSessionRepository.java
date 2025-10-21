package dev.skoleff.user_session_service.infrastructure.persistence;

import dev.skoleff.user_session_service.domain.model.UserSession;
import dev.skoleff.user_session_service.domain.repository.UserSessionRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
public class RedisUserSessionRepository implements UserSessionRepository {

    private final RedisTemplate<String, UserSession> redisTemplate;
    private static final Duration SESSION_TTL = Duration.ofMinutes(5);

    public RedisUserSessionRepository(RedisTemplate<String, UserSession> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(UserSession session) {
        redisTemplate.opsForValue().set("session:" + session.getSessionId(), session, SESSION_TTL);
    }

    @Override
    public Optional<UserSession> findById(String sessionId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get("session:" + sessionId));
    }

    @Override
    public void delete(String sessionId) {
        redisTemplate.delete("session:" + sessionId);
    }
}