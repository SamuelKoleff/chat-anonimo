package dev.skoleff.user_session_service.service;

import dev.skoleff.user_session_service.model.UserSession;
import dev.skoleff.user_session_service.model.Status;
import dev.skoleff.common_events.RoomCreatedEvent;
import dev.skoleff.common_events.UserMatchedEvent;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ReactiveSessionKafkaListener {

    private final ReactiveRedisOperations<String, UserSession> userSessionOps;

    public ReactiveSessionKafkaListener(ReactiveRedisOperations<String, UserSession> userSessionOps) {
        this.userSessionOps = userSessionOps;
    }

    @KafkaListener(topics = "room.created", groupId = "user-session-service")
    public void onRoomCreated(RoomCreatedEvent event) {
        String id1 = event.sessionId1();
        String id2 = event.sessionId2();
        String roomId = event.roomId();

        if (roomId == null) return;

        Mono<UserSession> update1 = userSessionOps.opsForValue()
                .get(id1)
                .flatMap(s -> userSessionOps.opsForValue()
                        .set(id1, s.withRoomId(roomId).withLastPingNow())
                        .thenReturn(s.withRoomId(roomId).withLastPingNow())
                );

        Mono<UserSession> update2 = userSessionOps.opsForValue()
                    .get(id2)
                    .flatMap(s -> userSessionOps.opsForValue()
                        .set(id2, s.withRoomId(roomId).withLastPingNow())
                        .thenReturn(s.withRoomId(roomId).withLastPingNow())
                );

        Mono.zip(update1, update2)
                .doOnSuccess(tuple -> System.out.println("room.created event: \n"
                        + tuple.getT1() + "\n" + tuple.getT2()))
                .subscribe();
    }

    @KafkaListener(topics = "user.matched", groupId = "user-session-service")
    public void onUserMatched(UserMatchedEvent event) {
        String id1 = event.sessionId1();
        String id2 = event.sessionId2();

        Mono<UserSession> update1 = userSessionOps.opsForValue()
                .get(id1)
                .flatMap(s -> userSessionOps.opsForValue()
                        .set(id1, s.withStatus(Status.MATCHED).withLastPingNow())
                        .thenReturn(s.withStatus(Status.MATCHED).withLastPingNow())
                );

        Mono<UserSession> update2 = userSessionOps.opsForValue()
                .get(id2)
                .flatMap(s -> userSessionOps.opsForValue()
                        .set(id2, s.withStatus(Status.MATCHED).withLastPingNow())
                        .thenReturn(s.withStatus(Status.MATCHED).withLastPingNow())
                );

        Mono.zip(update1, update2)
                .doOnSuccess(tuple -> System.out.println("user.matched event: \n"
                        + tuple.getT1() + "\n" + tuple.getT2()))
                .subscribe();
    }
}

