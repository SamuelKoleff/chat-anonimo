package dev.skoleff.chat_service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;

public class MyChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (command != null) {
            String sessionId = accessor.getSessionId();
            String destination = accessor.getDestination();

            LoggerFactory.getLogger(MyChannelInterceptor.class)
                    .info("STOMP Command={} SessionId={} Destination={}",
                        command, sessionId, destination
                    );
        }

        return message;
    }
}
