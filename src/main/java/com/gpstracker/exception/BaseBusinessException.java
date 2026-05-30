package com.gpstracker.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base class for all business exceptions
 * Subclasses should provide specific implementation details
 */
@Getter
public abstract class BaseBusinessException extends RuntimeException {
    
    private final String code;
    private final Object details;
    private final HttpStatus status;
    
    protected BaseBusinessException(String message, String code, HttpStatus status) {
        this(message, code, status, null);
    }
    
    protected BaseBusinessException(String message, String code, HttpStatus status, Object details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = details;
    }
} 