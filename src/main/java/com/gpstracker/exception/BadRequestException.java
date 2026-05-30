package com.gpstracker.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when the request is invalid
 */
public class BadRequestException extends BaseBusinessException {
    
    private static final String DEFAULT_CODE = "BAD_REQUEST";
    
    public BadRequestException(String message) {
        super(message, DEFAULT_CODE, HttpStatus.BAD_REQUEST);
    }
    
    public BadRequestException(String message, Object details) {
        super(message, DEFAULT_CODE, HttpStatus.BAD_REQUEST, details);
    }
} 