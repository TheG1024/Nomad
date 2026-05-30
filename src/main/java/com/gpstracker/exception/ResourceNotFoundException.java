package com.gpstracker.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested resource cannot be found
 */
public class ResourceNotFoundException extends BaseBusinessException {
    
    private static final String DEFAULT_CODE = "RESOURCE_NOT_FOUND";
    
    public ResourceNotFoundException(String message) {
        super(message, DEFAULT_CODE, HttpStatus.NOT_FOUND);
    }
    
    public ResourceNotFoundException(String resourceType, String identifier) {
        super(String.format("%s with identifier '%s' not found", resourceType, identifier),
              DEFAULT_CODE, 
              HttpStatus.NOT_FOUND,
              createDetails(resourceType, identifier));
    }
    
    private static Object createDetails(String resourceType, String identifier) {
        return new ResourceNotFoundDetails(resourceType, identifier);
    }
    
    private record ResourceNotFoundDetails(String resourceType, String identifier) {}
} 