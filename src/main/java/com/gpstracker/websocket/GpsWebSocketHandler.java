package com.gpstracker.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpstracker.model.GpsData;
import com.gpstracker.service.GpsDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
public class GpsWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private GpsDataService gpsDataService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            GpsData gpsData = objectMapper.readValue(message.getPayload(), GpsData.class);
            
            // Validate device ID from session attributes
            String deviceId = (String) session.getAttributes().get("deviceId");
            if (!deviceId.equals(gpsData.getDeviceId())) {
                log.error("Device ID mismatch for session {}", session.getId());
                session.close();
                return;
            }

            gpsDataService.saveGpsData(gpsData);
            log.debug("Received GPS data from device {}: {}", deviceId, gpsData);
        } catch (Exception e) {
            log.error("Error processing GPS data: ", e);
            session.close();
        }
    }
}
