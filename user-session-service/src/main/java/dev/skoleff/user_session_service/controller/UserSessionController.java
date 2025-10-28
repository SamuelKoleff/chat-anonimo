package dev.skoleff.user_session_service.controller;

import dev.skoleff.common_events.UserAvailableEvent;
import dev.skoleff.user_session_service.model.Status;
import dev.skoleff.user_session_service.model.UserSession;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;

@RestController
@RequestMapping("/sessions")
public class UserSessionController {
    private final KafkaTemplate kafkaTemplate;
    private final ReactiveRedisOperations<String, UserSession> userSessionOps;

    public UserSessionController(KafkaTemplate kafkaTemplate, ReactiveRedisOperations<String, UserSession> userSessionOps) {
        this.kafkaTemplate = kafkaTemplate;
        this.userSessionOps = userSessionOps;
    }

    @PostMapping
    public Mono<UserSession> createSession(@RequestBody UserSession session) {
        String key = session.sessionId();
        return userSessionOps.opsForValue()
                .set(key, session
                        .withStatus(Status.UP)
                        .withLastPingNow())
                .thenReturn(session);
    }

    @GetMapping("/{id}")
    public Mono<UserSession> getSession(@PathVariable("id") String sessionId) {
        return userSessionOps.opsForValue().get(sessionId);
    }

    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<UserSession> streamSession(@PathVariable("id") String sessionId) {
        return Flux.interval(Duration.ofSeconds(1))
                .flatMap(tick -> userSessionOps.opsForValue().get(sessionId))
                .filter(Objects::nonNull);
    }

    @PutMapping("/{id}/status")
    public Mono<Void> setStatus(@PathVariable("id") String sessionId, @RequestParam Status status) {
        return userSessionOps.opsForValue()
                .get(sessionId)
                .flatMap(s -> {
                    UserSession updated = s.withStatus(status).withLastPingNow();

                    Mono<Boolean> saveMono = userSessionOps.opsForValue().set(sessionId, updated);

                    Mono<Void> kafkaMono = Mono.fromRunnable(() -> {
                        if (status == Status.AVAILABLE) {
                            kafkaTemplate.send("user.available", new UserAvailableEvent(sessionId));
                            System.out.println("setting " + sessionId + " available and sending event");
                        }
                    });

                    return saveMono.then(kafkaMono);
                });
    }

    @PutMapping("/{id}/room")
    public Mono<Void> setRoom(@PathVariable("id") String sessionId, @RequestParam String roomId) {
        return userSessionOps.opsForValue()
                .get(sessionId)
                .flatMap(s -> userSessionOps.opsForValue()
                        .set(sessionId, s.withRoomId(roomId).withLastPingNow()))
                .then();
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteSession(@PathVariable("id") String sessionId) {
        return userSessionOps.opsForValue().delete(sessionId).then();
    }
}
