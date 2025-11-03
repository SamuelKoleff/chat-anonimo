package dev.skoleff.user_session_service.service;

import dev.skoleff.user_session_service.model.UserSession;
import io.lettuce.core.protocol.DemandAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Sinks;

@Configuration
public class SessionStreamConfig {

    @Bean
    public Sinks.Many<UserSession> userSessionSink(){
        return Sinks.many().multicast().onBackpressureBuffer();
    }
}
