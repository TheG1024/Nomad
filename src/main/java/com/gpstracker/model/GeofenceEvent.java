package com.gpstracker.model;

import java.time.Instant;

public class GeofenceEvent {
    private String deviceId;
    private String geofenceId;
    private String eventType; // ENTER or EXIT
    private long timestamp;

    public GeofenceEvent() {
        // Default constructor required for JSON deserialization
    }

    public GeofenceEvent(String deviceId, String geofenceId, String eventType) {
        this.deviceId = deviceId;
        this.geofenceId = geofenceId;
        this.eventType = eventType;
        this.timestamp = Instant.now().toEpochMilli();
    }

    // Getters and setters
    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getGeofenceId() {
        return geofenceId;
    }

    public void setGeofenceId(String geofenceId) {
        this.geofenceId = geofenceId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "GeofenceEvent{" +
                "deviceId='" + deviceId + '\'' +
                ", geofenceId='" + geofenceId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
} 