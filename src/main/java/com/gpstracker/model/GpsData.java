package com.gpstracker.model;

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
    private LocalDateTime timestamp;
    private String additionalInfo;
}
