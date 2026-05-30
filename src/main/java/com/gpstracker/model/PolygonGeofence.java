package com.gpstracker.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PolygonGeofence extends Geofence {
    private List<GeoPoint> vertices;
    
    @Override
    public boolean containsPoint(double latitude, double longitude) {
        // Ray casting algorithm for point-in-polygon detection
        boolean inside = false;
        int numVertices = vertices.size();
        
        if (numVertices < 3) {
            return false; // Not a valid polygon
        }
        
        int j = numVertices - 1;
        for (int i = 0; i < numVertices; i++) {
            GeoPoint vertexI = vertices.get(i);
            GeoPoint vertexJ = vertices.get(j);
            
            // Check if point is on an edge
            if (isPointOnEdge(latitude, longitude, vertexI, vertexJ)) {
                return true;
            }
            
            // Check if ray crosses edge
            if ((vertexI.getLatitude() > latitude) != (vertexJ.getLatitude() > latitude) &&
                (longitude < (vertexJ.getLongitude() - vertexI.getLongitude()) * 
                (latitude - vertexI.getLatitude()) / (vertexJ.getLatitude() - vertexI.getLatitude()) + 
                vertexI.getLongitude())) {
                inside = !inside;
            }
            
            j = i;
        }
        
        return inside;
    }
    
    private boolean isPointOnEdge(double lat, double lon, GeoPoint v1, GeoPoint v2) {
        // Check if point is on the edge defined by v1 and v2
        // Using a small epsilon for floating point comparison
        final double EPSILON = 1e-9;
        
        // Calculate distance from point to line segment
        double x = lon;
        double y = lat;
        double x1 = v1.getLongitude();
        double y1 = v1.getLatitude();
        double x2 = v2.getLongitude();
        double y2 = v2.getLatitude();
        
        double A = x - x1;
        double B = y - y1;
        double C = x2 - x1;
        double D = y2 - y1;
        
        double dot = A * C + B * D;
        double len_sq = C * C + D * D;
        double param = dot / len_sq;
        
        double xx, yy;
        
        if (param < 0 || (x1 == x2 && y1 == y2)) {
            xx = x1;
            yy = y1;
        } else if (param > 1) {
            xx = x2;
            yy = y2;
        } else {
            xx = x1 + param * C;
            yy = y1 + param * D;
        }
        
        double dx = x - xx;
        double dy = y - yy;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        // Convert distance to approximate meters
        double earthRadius = 6371000; // meters
        double distanceMeters = distance * (Math.PI / 180) * earthRadius;
        
        return distanceMeters < 10; // Consider point on edge if within 10 meters
    }
} 