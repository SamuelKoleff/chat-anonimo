package dev.skoleff.chat_service;

import dev.skoleff.common_events.UserDisconnectedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class MyChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(MyChannelInterceptor.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ConcurrentHashMap<String, String> sessionRoomMap = new ConcurrentHashMap<>();

    public MyChannelInterceptor(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (command != null) {
            String sessionId = accessor.getSessionId();
            String destination = accessor.getDestination();

            log.info("STOMP Command={} SessionId={} Destination={}", command, sessionId, destination);

            switch (command) {
                case SUBSCRIBE -> {
                    if (destination != null && destination.startsWith("/topic/match/")) {
                        String roomId = destination.substring("/topic/match/".length());
                        sessionRoomMap.put(sessionId, roomId);
                        log.info("Asociado session {} con room {}", sessionId, roomId);
                    }
                }

                case DISCONNECT, UNSUBSCRIBE -> {
                    String roomId = sessionRoomMap.remove(sessionId);
                    if (roomId != null) {
                        log.info("Usuario desconectado de room {}", roomId);
                        kafkaTemplate.send("user.disconnected", new UserDisconnectedEvent(roomId));
                    } else {
                        log.warn("No se encontró room para session {}", sessionId);
                    }
                }

                default -> {
                }
            }
        }

        return message;
    }
}
