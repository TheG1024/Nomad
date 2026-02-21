package com.gpstracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final List<String> allowedOrigins;

    public WebSocketConfig(@Value("${app.security.allowed-origins:}") String allowedOrigins) {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            this.allowedOrigins = Collections.emptyList();
        } else {
            this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .collect(Collectors.toList());
        }
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        if (allowedOrigins.isEmpty()) {
            registry.addEndpoint("/ws", "/gps").withSockJS();
        } else {
            registry.addEndpoint("/ws", "/gps")
                    .setAllowedOrigins(allowedOrigins.toArray(new String[0]))
                    .withSockJS();
        }
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic");
    }
}
