package com.gpstracker.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Model for time restrictions applied to geofences
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeRestriction implements Serializable {

    private String id;
    private String geofenceId;
    
    private LocalTime startTime;
    private LocalTime endTime;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime createdAt;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime updatedAt;
    
    /**
     * Check if the given time is within this restriction period
     * 
     * @param time The time to check
     * @return true if the time is within the restriction period
     */
    public boolean isWithinRestriction(LocalTime time) {
        // Handle cases that span midnight
        if (startTime.isAfter(endTime)) {
            return time.isAfter(startTime) || time.isBefore(endTime);
        }
        
        return time.isAfter(startTime) && time.isBefore(endTime);
    }
} 