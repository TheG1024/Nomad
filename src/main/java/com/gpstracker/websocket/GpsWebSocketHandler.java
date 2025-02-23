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
import java.time.LocalDateTime;
import java.util.HashMap;
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
            String messagePayload = message.getPayload();
            log.info("Server received: {}", messagePayload);

            // Use HashMap for deserialization
            HashMap<?, ?> value = objectMapper.readValue(messagePayload, HashMap.class);

            String deviceId = (String) value.get("deviceId");
            Double latitude = value.containsKey("latitude") ? Double.parseDouble(value.get("latitude").toString()) : null;
            Double longitude = value.containsKey("longitude") ? Double.parseDouble(value.get("longitude").toString()) : null;
            Double speed = value.containsKey("speed") ? Double.parseDouble(value.get("speed").toString()) : null;
            Double heading = value.containsKey("heading") ? Double.parseDouble(value.get("heading").toString()) : null;
            String timestampString = (String) value.get("timestamp");
            String additionalInfo = (String) value.get("additionalInfo");


            GpsData gpsData = GpsData.builder()
                    .deviceId(deviceId)
                    .latitude(latitude != null ? latitude : 0.0) // Default values
                    .longitude(longitude != null ? longitude : 0.0)
                    .speed(speed != null ? speed : 0.0)
                    .heading(heading != null ? heading : 0.0)
                    .timestamp(LocalDateTime.parse(timestampString))
                    .additionalInfo(additionalInfo)
                    .build();


            String sessionId = session.getId();
            
            // Handle heartbeat messages
            if (value.containsKey("type") && "heartbeat".equals(value.get("type"))) {
                handleHeartbeat(session);
                return;
            }

            // Handle device registration if not already registered
            if (!sessionToDeviceId.containsKey(sessionId)) {
                if (value.containsKey("deviceId")) {
                    deviceId = (String) value.get("deviceId");
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

            // Verify deviceId from session and message
            String sessionDeviceId = sessionToDeviceId.get(sessionId);
            if (sessionDeviceId == null || !sessionDeviceId.equals(gpsData.getDeviceId())) {
                sendError(session, "Device ID mismatch or session error");
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
