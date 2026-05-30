package com.gpstracker.exception;

/**
 * Exception thrown when a requested geofence cannot be found
 */
public class GeofenceNotFoundException extends RuntimeException {
    
    public GeofenceNotFoundException(String message) {
        super(message);
    }
    
    public GeofenceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public GeofenceNotFoundException(Long geofenceId) {
        super("Geofence not found with id: " + geofenceId);
    }
    
    public GeofenceNotFoundException(String field, String value) {
        super("Geofence not found with " + field + ": " + value);
    }
} 