package com.gpstracker.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class GpsData implements Serializable {
    private String deviceId;
    private double latitude;
    private double longitude;
    private double speed;
    private double heading;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime timestamp;
    private String additionalInfo;
}
