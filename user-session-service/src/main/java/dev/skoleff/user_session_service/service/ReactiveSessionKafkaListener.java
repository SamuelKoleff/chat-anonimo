package dev.skoleff.user_session_service.service;

import dev.skoleff.common_events.UserDisconnectedEvent;
import dev.skoleff.user_session_service.model.UserSession;
import dev.skoleff.user_session_service.model.Status;
import dev.skoleff.common_events.RoomCreatedEvent;
import dev.skoleff.common_events.UserMatchedEvent;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Service
public class ReactiveSessionKafkaListener {

    private final ReactiveRedisOperations<String, UserSession> userSessionOps;
    private final Sinks.Many<UserSession> updatesSink;


    public ReactiveSessionKafkaListener(ReactiveRedisOperations<String, UserSession> userSessionOps, Sinks.Many<UserSession> updatesSink) {
        this.userSessionOps = userSessionOps;
        this.updatesSink = updatesSink;
    }

    @KafkaListener(topics = "user.matched", groupId = "user-session-service")
    public void onUserMatched(UserMatchedEvent event) {
        String id1 = event.sessionId1();
        String id2 = event.sessionId2();

        Mono<UserSession> update1 = userSessionOps.opsForValue()
                .get(id1)
                .flatMap(s -> {
                    UserSession updated = s.withStatus(Status.MATCHED).withLastPingNow();
                    return userSessionOps.opsForValue().set(id1, updated)
                            .thenReturn(updated);
                });

        Mono<UserSession> update2 = userSessionOps.opsForValue()
                .get(id2)
                .flatMap(s -> {
                    UserSession updated = s.withStatus(Status.MATCHED).withLastPingNow();
                    return userSessionOps.opsForValue().set(id2, updated)
                            .thenReturn(updated);
                });

        Mono.zip(update1, update2)
                .doOnSuccess(tuple -> {
                    updatesSink.tryEmitNext(tuple.getT1());
                    updatesSink.tryEmitNext(tuple.getT2());
                })
                .subscribe();
    }


    @KafkaListener(topics = "room.created", groupId = "user-session-service")
    public void onRoomCreated(RoomCreatedEvent event) {
        String roomId = event.roomId();
        if (roomId == null) return;

        Mono<UserSession> update1 = userSessionOps.opsForValue()
                .get(event.sessionId1())
                .switchIfEmpty(Mono.error(new IllegalStateException("No existe session1: " + event.sessionId1())))
                .flatMap(s -> {
                    UserSession updated = s.withRoomId(roomId).withLastPingNow();
                    return userSessionOps.opsForValue().set(event.sessionId1(), updated)
                            .thenReturn(updated);
                });

        Mono<UserSession> update2 = userSessionOps.opsForValue()
                .get(event.sessionId2())
                .switchIfEmpty(Mono.error(new IllegalStateException("No existe session2: " + event.sessionId2())))
                .flatMap(s -> {
                    UserSession updated = s.withRoomId(roomId).withLastPingNow();
                    return userSessionOps.opsForValue().set(event.sessionId2(), updated)
                            .thenReturn(updated);
                });

        Mono.zip(update1, update2)
                .doOnSuccess(tuple -> {
                    updatesSink.tryEmitNext(tuple.getT1());
                    updatesSink.tryEmitNext(tuple.getT2());
                })
                .doOnError(err -> {
                    System.err.println("ERROR en zip: " + err.getMessage());
                })
                .subscribe();
    }

    @KafkaListener(topics = "user.disconnected", groupId = "user-session-service")
    public void onUserDisconneted(UserDisconnectedEvent event){
        System.out.println("user.disconneted event: " + event.toString());
        String roomId = event.matchId();
        
        userSessionOps.scan()
                .flatMap(key -> userSessionOps.opsForValue().get(key)
                        .filter(session -> roomId.equals(session.roomId())))
                .flatMap(session -> {
                    UserSession updated = session.withStatus(Status.UP).withRoomId(null).withLastPingNow();
                    return userSessionOps.opsForValue().set(session.sessionId(), updated)
                            .thenReturn(updated);
                })
                .doOnNext(updated -> {
                    updatesSink.tryEmitNext(updated);
                    System.out.println("Usuario devuelto a estado UP: " + updated.sessionId());
                })
                .doOnError(e -> System.err.println("Error restaurando usuarios del room: " + e.getMessage()))
                .subscribe();

    }

}

