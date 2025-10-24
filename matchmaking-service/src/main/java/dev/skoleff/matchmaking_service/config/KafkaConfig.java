package dev.skoleff.matchmaking_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic userAvailableTopic() {
        return new NewTopic("user.available", 1, (short) 1);
    }

    @Bean
    public NewTopic userMatchedTopic() {
        return new NewTopic("user.matched", 1, (short) 1);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> factory) {
        return new KafkaTemplate<>(factory);
    }
}

