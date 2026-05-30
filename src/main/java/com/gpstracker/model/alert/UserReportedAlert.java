package com.gpstracker.model.alert;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Waze-style user-reported alerts (police, hazards, accidents, etc.)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserReportedAlert {
    
    private String id;
    private AlertType type;
    private AlertSubtype subtype;
    private Double latitude;
    private Double longitude;
    private String street;
    private Integer reliability;
    private Integer confidence;
    private String description;
    private String reportedBy;
    private Instant reportedAt;
    private Integer upvotes;
    private Integer downvotes;
    private Map<String, Object> metadata;
    private AlertStatus status;
    
    public enum AlertType {
        POLICE,
        HAZARD,
        ACCIDENT,
        TRAFFIC,
        ROAD_CLOSED,
        CONSTRUCTION,
        WEATHER,
        OTHER
    }
    
    public enum AlertSubtype {
        // Police
        POLICE_VISIBLE,
        POLICE_HIDDEN,
        SPEED_TRAP,
        RED_LIGHT_CAMERA,
        
        // Hazard
        HAZARD_ON_ROAD,
        HAZARD_ON_SHOULDER,
        HAZARD_WEATHER,
        CAR_STOPPED,
        ANIMAL_ON ROAD,
        TOLL_BOOTH,
        
        // Accident
        ACCIDENT_MINOR,
        ACCIDENT_MAJOR,
        ACCIDENT_FATAL,
        
        // Traffic
        TRAFFIC_LIGHT,
        TRAFFIC_HEAVY,
        TRAFFIC_STANDSTILL,
        
        // Road closed
        ROAD_CLOSED_CONSTRUCTION,
        ROAD_CLOSED_EVENT,
        ROAD_CLOSED_HAZARD,
        
        // Construction
        CONSTRUCTION_MINOR,
        CONSTRUCTION_MAJOR,
        
        // Weather
        WEATHER_FOG,
        WEATHER_RAIN,
        WEATHER_SNOW,
        WEATHER_ICE
    }
    
    public enum AlertStatus {
        ACTIVE,
        CONFIRMED,
        EXPIRED,
        REJECTED
    }
    
    public double getScore() {
        // Waze-style scoring: reliability * confidence * (upvotes - downvotes)
        int voteScore = Math.max(0, upvotes - downvotes);
        return (reliability != null ? reliability : 5) * 
               (confidence != null ? confidence : 5) * 
               (1 + voteScore * 0.5);
    }
    
    public boolean isExpired() {
        // Alerts expire after 2 hours by default
        return Instant.now().minusSeconds(7200).isAfter(reportedAt);
    }
}