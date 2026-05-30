package com.gpstracker.exception;

/**
 * Exception thrown when a requested device cannot be found
 */
public class DeviceNotFoundException extends RuntimeException {
    
    public DeviceNotFoundException(String message) {
        super(message);
    }
    
    public DeviceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public DeviceNotFoundException(Long deviceId) {
        super("Device not found with id: " + deviceId);
    }
    
    public DeviceNotFoundException(String field, String value) {
        super("Device not found with " + field + ": " + value);
    }
} 