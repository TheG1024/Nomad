package com.gpstracker.config;

import com.gpstracker.controller.WebSocketController;
import com.gpstracker.model.PoliceAlert;
import com.gpstracker.service.PoliceAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Bridge between PoliceAlertService and WebSocket broadcasting.
 * Broadcasts existing alerts on startup and listens for updates.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PoliceAlertWebSocketBridge {

    private final PoliceAlertService policeAlertService;
    private final WebSocketController webSocketController;

    @PostConstruct
    public void initialize() {
        log.info("Initializing PoliceAlert WebSocket bridge...");
        
        // Broadcast all active alerts on startup
        List<PoliceAlert> activeAlerts = policeAlertService.getActiveAlerts();
        log.info("Broadcasting {} active police alerts on startup", activeAlerts.size());
        activeAlerts.forEach(alert -> 
            webSocketController.broadcastPoliceAlert(alert)
        );
        
        log.info("PoliceAlert WebSocket bridge initialized successfully");
    }
}