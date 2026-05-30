package com.gpstracker.model;

import java.time.Instant;

public class DeviceUpdate {
    private String deviceId;
    private double latitude;
    private double longitude;
    private String status;
    private long timestamp;

    public DeviceUpdate() {
        // Default constructor required for JSON deserialization
    }

    public DeviceUpdate(String deviceId, double latitude, double longitude, String status) {
        this.deviceId = deviceId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
        this.timestamp = Instant.now().toEpochMilli();
    }

    // Getters and setters
    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "DeviceUpdate{" +
                "deviceId='" + deviceId + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
} 