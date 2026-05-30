package com.gpstracker.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CircleGeofence extends Geofence {
    private double centerLatitude;
    private double centerLongitude;
    private double radiusMeters;
    
    @Override
    public boolean containsPoint(double latitude, double longitude) {
        // Haversine formula to calculate distance
        double earthRadius = 6371000; // meters
        double dLat = Math.toRadians(latitude - centerLatitude);
        double dLon = Math.toRadians(longitude - centerLongitude);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(centerLatitude)) * Math.cos(Math.toRadians(latitude)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = earthRadius * c;
        
        return distance <= radiusMeters;
    }
} 