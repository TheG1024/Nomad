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
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GpsData implements Serializable {
    private String deviceId;
    private double latitude;
    private double longitude;
    private double speed;
    private double heading;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime timestamp;
    
    // Device status
    private double batteryLevel;
    private String deviceStatus; // ACTIVE, IDLE, OFFLINE
    private double accuracy; // GPS accuracy in meters
    
    // Alert flags
    private boolean lowBattery;
    private boolean geofenceAlert;
    private boolean speedAlert;
    private boolean malfunctionAlert;
    
    // Environmental data
    private double altitude;
    private double temperature;
    private double humidity;
    
    // Network info
    private String networkType; // GSM, WIFI, etc.
    private int signalStrength;
    
    // Extended data
    private Map<String, Object> metadata;
    private String additionalInfo;
}
