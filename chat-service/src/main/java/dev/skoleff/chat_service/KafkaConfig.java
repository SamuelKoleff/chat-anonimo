package dev.skoleff.chat_service;


import org.apache.kafka.clients.admin.NewTopic;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic userMatchedTopic() {
        return new NewTopic("user.matched", 1, (short) 1);
    }
}
