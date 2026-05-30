package com.gpstracker.controller;

import com.gpstracker.dto.DeviceUpdateDTO;
import com.gpstracker.dto.GeofenceEventDTO;
import com.gpstracker.model.alert.UserReportedAlert;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;

import com.gpstracker.model.DeviceUpdate;
import com.gpstracker.model.GeofenceEvent;
import com.gpstracker.model.PoliceAlert;
import com.gpstracker.model.Notification;

/**
 * Controller for handling WebSocket messages for real-time updates
 */
@Controller
@Slf4j
public class WebSocketController {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * Handle device location updates sent from clients
     * @param update the device update data
     * @return the processed update to be broadcast to all subscribers
     */
    @MessageMapping("/device/update")
    @SendTo("/topic/device/updates")
    public DeviceUpdate processDeviceUpdate(DeviceUpdate update) {
        // Log the received update
        System.out.println("Received device update: " + update);
        
        // In a real implementation, you would process and store the update
        
        // Return the update to be broadcast to all subscribers
        return update;
    }
    
    /**
     * Method for server to broadcast device updates to clients
     * @param deviceId the device ID
     * @param latitude the new latitude
     * @param longitude the new longitude
     */
    public void broadcastDeviceUpdate(String deviceId, double latitude, double longitude) {
        DeviceUpdateDTO update = new DeviceUpdateDTO();
        update.setDeviceId(deviceId);
        update.setLatitude(latitude);
        update.setLongitude(longitude);
        update.setTimestamp(System.currentTimeMillis());
        update.setStatus("online");
        
        log.debug("Broadcasting device update: {}", update);
        messagingTemplate.convertAndSend("/topic/device/updates", update);
    }
    
    /**
     * Handle geofence entry/exit events
     * @param event the geofence event data
     * @return the processed event to be broadcast to all subscribers
     */
    @MessageMapping("/geofence/event")
    @SendTo("/topic/geofence/events")
    public GeofenceEvent processGeofenceEvent(GeofenceEvent event) {
        // Log the received event
        System.out.println("Received geofence event: " + event);
        
        // In a real implementation, you would process and store the event
        
        // Return the event to be broadcast to all subscribers
        return event;
    }
    
    /**
     * Method for server to broadcast geofence events to clients
     * @param deviceId the device ID
     * @param geofenceId the geofence ID
     * @param eventType the event type (ENTER, EXIT)
     */
    public void broadcastGeofenceEvent(String deviceId, String geofenceId, String eventType) {
        GeofenceEventDTO event = new GeofenceEventDTO();
        event.setDeviceId(deviceId);
        event.setGeofenceId(geofenceId);
        event.setEventType(eventType);
        event.setTimestamp(System.currentTimeMillis());
        
        log.debug("Broadcasting geofence event: {}", event);
        messagingTemplate.convertAndSend("/topic/geofence/events", event);
        
        // Also send a system notification
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "GEOFENCE_" + eventType);
        notification.put("deviceId", deviceId);
        notification.put("geofenceId", geofenceId);
        notification.put("message", "Device " + deviceId + " " + eventType.toLowerCase() + "ed geofence " + geofenceId);
        notification.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSend("/topic/notifications", notification);
    }
    
    /**
     * Send a system notification
     * @param type the notification type
     * @param message the notification message
     */
    public void sendNotification(String type, String message) {
        Notification notification = new Notification(type, message);
        messagingTemplate.convertAndSend("/topic/notifications", notification);
    }
    
    /**
     * Broadcast a police alert update to all clients
     * @param alert the police alert to broadcast
     */
    public void broadcastPoliceAlert(PoliceAlert alert) {
        log.info("Broadcasting police alert: {} - {} at ({}, {})", 
            alert.getName(), alert.getAlertType(), alert.getLatitude(), alert.getLongitude());
        messagingTemplate.convertAndSend("/topic/police-alerts", alert);
        
        // Also send a notification for high/critical severity alerts
        if ("HIGH".equals(alert.getSeverity()) || "CRITICAL".equals(alert.getSeverity())) {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "POLICE_ALERT_" + alert.getSeverity());
            notification.put("alertId", alert.getId());
            notification.put("title", alert.getName());
            notification.put("message", alert.getDescription());
            notification.put("severity", alert.getSeverity());
            notification.put("alertType", alert.getAlertType());
            notification.put("latitude", alert.getLatitude());
            notification.put("longitude", alert.getLongitude());
            notification.put("timestamp", System.currentTimeMillis());
            messagingTemplate.convertAndSend("/topic/notifications", notification);
        }
    }
    
    /**
     * Broadcast a user-reported alert (Waze-style) to all clients
     * @param alert the user-reported alert to broadcast
     */
    public void broadcastUserAlert(UserReportedAlert alert) {
        log.info("Broadcasting user alert: {} - {} at ({}, {})", 
            alert.getType(), alert.getSubtype(), alert.getLatitude(), alert.getLongitude());
        messagingTemplate.convertAndSend("/topic/user-alerts", alert);
    }
    
    /**
     * Broadcast alert removal to all clients
     * @param alertId the ID of the removed alert
     */
    public void broadcastAlertRemoved(String alertId) {
        Map<String, Object> removal = new HashMap<>();
        removal.put("alertId", alertId);
        removal.put("action", "removed");
        messagingTemplate.convertAndSend("/topic/user-alerts", removal);
    }
} 