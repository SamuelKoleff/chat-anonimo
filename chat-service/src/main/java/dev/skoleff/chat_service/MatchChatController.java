package dev.skoleff.chat_service;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class MatchChatController {



    @MessageMapping("/match/{matchId}")
    @SendTo("/topic/match/{matchId}")
    public ChatMessage sendMessage(@DestinationVariable String matchId, ChatMessage message) {
        System.out.printf("Mensaje en match %s: %s%n", matchId, message.getContent());
        return message;
    }
}
