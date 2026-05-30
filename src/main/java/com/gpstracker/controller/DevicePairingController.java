package com.gpstracker.controller;

import com.gpstracker.dto.DeviceUpdateDTO;
import com.gpstracker.service.DeviceSimulationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Universal Device Pairing API
 * Accepts GPS data from ANY source:
 * - Mobile apps (OwnTracks, GPS Logger, Traccar)
 * - Hardware trackers (TK103, GT06, Concox)
 * - Custom IoT devices (ESP32, Arduino)
 * - Web browsers (navigator.geolocation)
 */
@RestController
@RequestMapping("/api/devices")
@Slf4j
@CrossOrigin(origins = "*")
public class DevicePairingController {
    
    @Autowired
    private DeviceSimulationService simulationService;
    
    @Autowired
    private WebSocketController webSocketController;
    
    // Store registered devices
    private static final Map<String, RegisteredDevice> registeredDevices = new ConcurrentHashMap<>();
    
    /**
     * Register a new device and get an API key
     * POST /api/devices/register
     * Body: { "name": "My Phone", "type": "mobile_app" }
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerDevice(@RequestBody Map<String, String> request) {
        String deviceId = UUID.randomUUID().toString();
        String apiKey = UUID.randomUUID().toString().replace("-", "");
        String name = request.getOrDefault("name", "Unknown Device");
        String type = request.getOrDefault("type", "generic");
        
        RegisteredDevice device = new RegisteredDevice();
        device.setId(deviceId);
        device.setApiKey(apiKey);
        device.setName(name);
        device.setType(type);
        device.setActive(true);
        
        registeredDevices.put(deviceId, device);
        
        log.info("New device registered: {} ({}) - Type: {}", name, deviceId, type);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("deviceId", deviceId);
        response.put("apiKey", apiKey);
        response.put("name", name);
        response.put("message", "Device registered successfully. Use this apiKey to send location updates.");
        response.put("endpoints", Map.of(
            "http_post", "/api/devices/{deviceId}/location?apiKey={apiKey}",
            "websocket", "/ws (subscribe to /topic/device/updates)",
            "get_location", "/api/devices/{deviceId}/status?apiKey={apiKey}"
        ));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Receive GPS location from ANY device
     * POST /api/devices/{deviceId}/location?apiKey=YOUR_KEY
     * Body: { "latitude": 40.7128, "longitude": -74.006, "speed": 25.5, "heading": 45, "accuracy": 10 }
     * 
     * OR query params:
     * ?lat=40.7128&lng=-74.006&speed=25.5&heading=45
     */
    @PostMapping("/{deviceId}/location")
    public ResponseEntity<?> receiveLocation(
            @PathVariable String deviceId,
            @RequestParam String apiKey,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double speed,
            @RequestParam(required = false) Double heading,
            @RequestParam(required = false) Double accuracy
    ) {
        // Validate API key
        RegisteredDevice device = registeredDevices.get(deviceId);
        if (device == null || !device.getApiKey().equals(apiKey)) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Invalid device ID or API key"
            ));
        }
        
        double latitude, longitude;
        
        // Support both JSON body and query params
        if (body != null && body.containsKey("latitude")) {
            latitude = ((Number) body.get("latitude")).doubleValue();
            longitude = ((Number) body.get("longitude")).doubleValue();
            speed = body.containsKey("speed") ? ((Number) body.get("speed")).doubleValue() : null;
            heading = body.containsKey("heading") ? ((Number) body.get("heading")).doubleValue() : null;
        } else if (lat != null && lng != null) {
            latitude = lat;
            longitude = lng;
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Missing location data. Provide lat/lng in query params or JSON body"
            ));
        }
        
        // Broadcast to all connected clients
        webSocketController.broadcastDeviceUpdate(deviceId, latitude, longitude);
        
        // Update device status
        device.setLastLatitude(latitude);
        device.setLastLongitude(longitude);
        device.setLastSpeed(speed);
        device.setLastHeading(heading);
        device.setLastUpdate(System.currentTimeMillis());
        
        log.debug("Location received from {}: ({}, {})", deviceId, latitude, longitude);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Location updated",
            "deviceId", deviceId,
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * Get device status and last known location
     * GET /api/devices/{deviceId}/status?apiKey=YOUR_KEY
     */
    @GetMapping("/{deviceId}/status")
    public ResponseEntity<?> getDeviceStatus(
            @PathVariable String deviceId,
            @RequestParam String apiKey
    ) {
        RegisteredDevice device = registeredDevices.get(deviceId);
        if (device == null || !device.getApiKey().equals(apiKey)) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Invalid device ID or API key"
            ));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("device", Map.of(
            "id", device.getId(),
            "name", device.getName(),
            "type", device.getType(),
            "active", device.isActive(),
            "lastLocation", Map.of(
                "latitude", device.getLastLatitude(),
                "longitude", device.getLastLongitude(),
                "speed", device.getLastSpeed(),
                "heading", device.getLastHeading(),
                "timestamp", device.getLastUpdate()
            ),
            "registeredAt", device.getRegisteredAt()
        ));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * List all registered devices
     * GET /api/devices/list
     */
    @GetMapping("/list")
    public ResponseEntity<?> listDevices() {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "count", registeredDevices.size(),
            "devices", registeredDevices.values().stream()
                .map(d -> Map.of(
                    "id", d.getId(),
                    "name", d.getName(),
                    "type", d.getType(),
                    "active", d.isActive(),
                    "lastSeen", d.getLastUpdate()
                ))
                .toList()
        ));
    }
    
    /**
     * Simple GPS endpoint for basic devices (no auth, use for testing only)
     * GET /api/gps/update?device=MyPhone&lat=40.7128&lng=-74.006
     */
    @GetMapping("/gps/update")
    public ResponseEntity<?> simpleGpsUpdate(
            @RequestParam String device,
            @RequestParam Double lat,
            @RequestParam Double lng
    ) {
        // Auto-register if not exists
        String deviceId = "simple_" + device.replace(" ", "_");
        RegisteredDevice existing = registeredDevices.get(deviceId);
        if (existing == null) {
            RegisteredDevice newDevice = new RegisteredDevice();
            newDevice.setId(deviceId);
            newDevice.setApiKey("public");
            newDevice.setName(device);
            newDevice.setType("simple_gps");
            newDevice.setActive(true);
            registeredDevices.put(deviceId, newDevice);
            log.info("Auto-registered simple GPS device: {}", device);
        }
        
        // Broadcast location
        webSocketController.broadcastDeviceUpdate(deviceId, lat, lng);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "GPS location updated",
            "device", device,
            "location", Map.of("lat", lat, "lng", lng)
        ));
    }
    
    // Inner class for registered devices
    public static class RegisteredDevice {
        private String id;
        private String apiKey;
        private String name;
        private String type;
        private boolean active;
        private Double lastLatitude;
        private Double lastLongitude;
        private Double lastSpeed;
        private Double lastHeading;
        private Long lastUpdate;
        private Long registeredAt = System.currentTimeMillis();
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public Double getLastLatitude() { return lastLatitude; }
        public void setLastLatitude(Double latitude) { this.lastLatitude = latitude; }
        public Double getLastLongitude() { return lastLongitude; }
        public void setLastLongitude(Double longitude) { this.lastLongitude = longitude; }
        public Double getLastSpeed() { return lastSpeed; }
        public void setLastSpeed(Double speed) { this.lastSpeed = speed; }
        public Double getLastHeading() { return lastHeading; }
        public void setLastHeading(Double heading) { this.lastHeading = heading; }
        public Long getLastUpdate() { return lastUpdate; }
        public void setLastUpdate(Long lastUpdate) { this.lastUpdate = lastUpdate; }
        public Long getRegisteredAt() { return registeredAt; }
        public void setRegisteredAt(Long registeredAt) { this.registeredAt = registeredAt; }
    }
}