package com.gpstracker.websocket;

import com.gpstracker.model.GpsData;
import com.gpstracker.service.GpsDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
public class GpsWebSocketController {

    @Autowired
    private GpsDataService gpsDataService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/gps")
    @SendTo("/topic/updates")
    public GpsData handleGpsData(GpsData gpsData) {
        try {
            // Save the GPS data
            gpsDataService.saveGpsData(gpsData);
            
            // Send device-specific updates
            messagingTemplate.convertAndSend(
                "/topic/device/" + gpsData.getDeviceId(),
                gpsData
            );

            // Send alerts if any are triggered
            if (gpsData.isLowBattery() || gpsData.isSpeedAlert() || 
                gpsData.isGeofenceAlert() || gpsData.isMalfunctionAlert()) {
                messagingTemplate.convertAndSend(
                    "/topic/alerts/" + gpsData.getDeviceId(),
                    gpsData
                );
            }

            return gpsData;
        } catch (Exception e) {
            log.error("Error handling GPS data via WebSocket: ", e);
            throw e;
        }
    }

    @MessageMapping("/subscribe")
    public void subscribeToDevice(String deviceId) {
        log.info("New subscription for device: {}", deviceId);
    }

    @MessageMapping("/unsubscribe")
    public void unsubscribeFromDevice(String deviceId) {
        log.info("Unsubscribed from device: {}", deviceId);
    }
}
