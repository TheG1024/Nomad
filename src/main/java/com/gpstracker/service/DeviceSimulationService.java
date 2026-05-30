package com.gpstracker.service;

import com.gpstracker.controller.WebSocketController;
import com.gpstracker.model.Geofence;
import com.gpstracker.service.GeofenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Service to simulate device movements and geofence events for demonstration
 */
@Service
@EnableScheduling
@Slf4j
public class DeviceSimulationService {

    @Autowired
    private WebSocketController webSocketController;

    @Autowired
    private GeofenceService geofenceService;

    // Store last positions of simulated devices
    private final Map<String, DevicePosition> devicePositions = new ConcurrentHashMap<>();

    // Store which devices are inside which geofences (deviceId -> geofenceId ->
    // isInside)
    private final Map<String, Set<String>> deviceInGeofences = new ConcurrentHashMap<>();

    /**
     * Initialize simulation data
     */
    public DeviceSimulationService() {
        // Initialize some device positions
        devicePositions.put("dev-001", new DevicePosition(40.7128, -74.006));
        devicePositions.put("dev-002", new DevicePosition(34.0522, -118.2437));
        devicePositions.put("dev-003", new DevicePosition(51.5074, -0.1278));

        // Initial state for geofence tracking
        for (String deviceId : devicePositions.keySet()) {
            deviceInGeofences.put(deviceId, new HashSet<>());
        }
    }

    /**
     * Periodically simulate device movements and check geofence statuses
     */
    @Scheduled(fixedRate = 5000) // Run every 5 seconds
    public void simulateDeviceMovements() {
        for (Map.Entry<String, DevicePosition> entry : devicePositions.entrySet()) {
            String deviceId = entry.getKey();
            DevicePosition position = entry.getValue();

            // Skip offline devices
            if (deviceId.equals("dev-003")) {
                continue; // This device is offline
            }

            // Generate a random movement
            double deltaLat = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.005;
            double deltaLng = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.005;

            double newLat = position.latitude + deltaLat;
            double newLng = position.longitude + deltaLng;

            // Update position
            position.latitude = newLat;
            position.longitude = newLng;

            // Broadcast the update
            webSocketController.broadcastDeviceUpdate(deviceId, newLat, newLng);

            // Check geofence statuses
            checkGeofenceStatus(deviceId, newLat, newLng);
        }
    }

    /**
     * Check if a device's new position crosses any geofence boundaries
     * Delegates logic to GeofenceService
     */
    private void checkGeofenceStatus(String deviceId, double lat, double lng) {
        try {
            // Get currently triggered geofences from the service
            List<Geofence> currentTriggeredFences = geofenceService.checkGeofences(deviceId, lat, lng);
            Set<String> currentFenceIds = currentTriggeredFences.stream()
                    .map(Geofence::getId)
                    .collect(Collectors.toSet());

            Set<String> previousFenceIds = deviceInGeofences.getOrDefault(deviceId, new HashSet<>());

            // Check for ENTERS (in current but not in previous)
            for (String fenceId : currentFenceIds) {
                if (!previousFenceIds.contains(fenceId)) {
                    log.info("Device {} entered geofence {}", deviceId, fenceId);
                    webSocketController.broadcastGeofenceEvent(deviceId, fenceId, "ENTER");
                }
            }

            // Check for EXITS (in previous but not in current)
            for (String fenceId : previousFenceIds) {
                if (!currentFenceIds.contains(fenceId)) {
                    log.info("Device {} exited geofence {}", deviceId, fenceId);
                    webSocketController.broadcastGeofenceEvent(deviceId, fenceId, "EXIT");
                }
            }

            // Update the tracked state
            deviceInGeofences.put(deviceId, currentFenceIds);

        } catch (Exception e) {
            log.error("Error checking geofence status for device {}: {}", deviceId, e.getMessage());
        }
    }

    /**
     * Calculate distance between two points using Haversine formula (meters)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth's radius in kilometers

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c * 1000; // Return in meters
    }

    /**
     * Inner class to represent a device position
     */
    private static class DevicePosition {
        public double latitude;
        public double longitude;

        public DevicePosition(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}