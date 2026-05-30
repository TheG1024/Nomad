package com.gpstracker.config;

import com.gpstracker.controller.WebSocketController;
import com.gpstracker.service.PoliceAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * Configuration for police alert system integration.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class PoliceAlertConfig {

    private final PoliceAlertService policeAlertService;
    private final WebSocketController webSocketController;

    /**
     * Initialize police alert service with WebSocket integration.
     * This bean ensures alerts are broadcast when created/updated.
     */
    @Bean
    @DependsOn("webSocketController")
    public PoliceAlertWebSocketBridge policeAlertWebSocketBridge() {
        return new PoliceAlertWebSocketBridge(policeAlertService, webSocketController);
    }
}