package com.gpstracker.model;

import java.time.Instant;

public class Notification {
    private String type;
    private String message;
    private long timestamp;

    public Notification() {
        // Default constructor required for JSON deserialization
    }

    public Notification(String type, String message) {
        this.type = type;
        this.message = message;
        this.timestamp = Instant.now().toEpochMilli();
    }

    // Getters and setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "type='" + type + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
} 