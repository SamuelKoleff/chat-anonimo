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
import reactor.core.publisher.Sinks;

@RestController
@RequestMapping("/sessions")
public class UserSessionController {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ReactiveRedisOperations<String, UserSession> userSessionOps;
    private final Sinks.Many<UserSession> updatesSink;


    public UserSessionController(KafkaTemplate<String,Object> kafkaTemplate, ReactiveRedisOperations<String, UserSession> userSessionOps, Sinks.Many<UserSession> updatesSink) {
        this.kafkaTemplate = kafkaTemplate;
        this.userSessionOps = userSessionOps;
        this.updatesSink = updatesSink;
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
        return updatesSink.asFlux()
                .filter(s -> s.sessionId().equals(sessionId))
                .doOnSubscribe(sub -> System.out.println("Client is listening " + sessionId));
    }


    @PutMapping("/{id}/status")
    public Mono<Void> setStatus(@PathVariable("id") String sessionId, @RequestParam Status status) {
        return userSessionOps.opsForValue()
                .get(sessionId)
                .flatMap(s -> {
                    UserSession updated = s.withStatus(status).withLastPingNow();

                    return userSessionOps.opsForValue()
                            .set(sessionId, updated)
                            .then(Mono.fromRunnable(() -> {
                                updatesSink.tryEmitNext(updated);
                                if (status == Status.AVAILABLE) {
                                    kafkaTemplate.send("user.available", new UserAvailableEvent(sessionId));
                                }
                            }));
                });
    }


    @PutMapping("/{id}/room")
    public Mono<Void> setRoom(@PathVariable("id") String sessionId, @RequestParam String roomId) {
        return userSessionOps.opsForValue()
                .get(sessionId)
                .flatMap(s -> {
                    UserSession updated = s.withRoomId(roomId).withLastPingNow();
                    return userSessionOps.opsForValue().set(sessionId, updated)
                            .then(Mono.fromRunnable(() -> updatesSink.tryEmitNext(updated)));
                });
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteSession(@PathVariable("id") String sessionId) {
        return userSessionOps.opsForValue().delete(sessionId).then();
    }
}
