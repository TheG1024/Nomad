package com.gpstracker.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CircleGeofence.class, name = "circle"),
        @JsonSubTypes.Type(value = PolygonGeofence.class, name = "polygon")
})
public abstract class Geofence implements Serializable {
    private String id;
    private String deviceId;
    private String name;
    private String description;
    private String category; // home, work, restricted, etc.
    private int alertLevel; // 1=info, 2=warning, 3=critical

    // Time restrictions
    private boolean timeRestricted;
    private LocalTime activeStartTime;
    private LocalTime activeEndTime;

    // Status and metadata
    private boolean active;
    private String color; // For UI display

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime updatedAt;

    // Abstract method that subclasses must implement to check if a point is inside
    // this geofence
    public abstract boolean containsPoint(double latitude, double longitude);

    // Check if geofence is active based on time restrictions
    public boolean isActiveAtTime(LocalTime time) {
        if (!timeRestricted || !active) {
            return active;
        }

        // Handle cases that span midnight
        if (activeStartTime.isAfter(activeEndTime)) {
            return !time.isBefore(activeStartTime) || !time.isAfter(activeEndTime);
        }

        // Inclusive boundary: active from startTime up to and including endTime
        return !time.isBefore(activeStartTime) && !time.isAfter(activeEndTime);
    }

    // Generate a unique ID for new geofences
    public void generateId() {
        if (this.id == null || this.id.isEmpty()) {
            this.id = UUID.randomUUID().toString();
        }
    }
}