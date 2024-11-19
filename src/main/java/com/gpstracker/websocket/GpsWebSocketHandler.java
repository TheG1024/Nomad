package com.gpstracker.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gpstracker.model.GpsData;
import com.gpstracker.service.GpsDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class GpsWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToDeviceId = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    
    @Autowired
    private GpsDataService gpsDataService;

    public GpsWebSocketHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        log.info("New WebSocket connection established. Session ID: {}", sessionId);
        sessions.put(sessionId, session);
        
        // Send welcome message
        sendMessage(session, Map.of(
            "type", "welcome",
            "message", "Connection established. Please send device ID."
        ));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String sessionId = session.getId();
            
            // Handle heartbeat messages
            if (payload.containsKey("type") && "heartbeat".equals(payload.get("type"))) {
                handleHeartbeat(session);
                return;
            }

            // Handle device registration if not already registered
            if (!sessionToDeviceId.containsKey(sessionId)) {
                if (payload.containsKey("deviceId")) {
                    String deviceId = payload.get("deviceId").toString();
                    sessionToDeviceId.put(sessionId, deviceId);
                    log.info("Device {} registered with session {}", deviceId, sessionId);
                    sendMessage(session, Map.of(
                        "type", "registration",
                        "status", "success",
                        "deviceId", deviceId
                    ));
                    return;
                } else {
                    sendError(session, "Device ID not provided");
                    return;
                }
            }

            // Process GPS data
            GpsData gpsData = objectMapper.convertValue(payload, GpsData.class);
            String deviceId = sessionToDeviceId.get(sessionId);
            
            if (!deviceId.equals(gpsData.getDeviceId())) {
                sendError(session, "Device ID mismatch");
                return;
            }

            gpsDataService.saveGpsData(gpsData);
            sendMessage(session, Map.of(
                "type", "ack",
                "status", "received",
                "timestamp", gpsData.getTimestamp().toString()
            ));
            
            log.debug("Processed GPS data from device {}: {}", deviceId, gpsData);

        } catch (Exception e) {
            log.error("Error processing message: ", e);
            sendError(session, "Error processing message: " + e.getMessage());
        }
    }

    private void handleHeartbeat(WebSocketSession session) {
        sendMessage(session, Map.of(
            "type", "heartbeat",
            "status", "received"
        ));
    }

    private void sendError(WebSocketSession session, String message) {
        sendMessage(session, Map.of(
            "type", "error",
            "message", message
        ));
    }

    private void sendMessage(WebSocketSession session, Map<String, Object> message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(jsonMessage));
        } catch (IOException e) {
            log.error("Error sending message to session {}", session.getId(), e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        sessionToDeviceId.remove(sessionId);
        log.info("WebSocket connection closed. Session ID: {}", sessionId);
    }
}
