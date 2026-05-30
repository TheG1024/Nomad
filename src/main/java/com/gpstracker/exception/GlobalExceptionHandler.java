package com.gpstracker.exception;

import com.gpstracker.dto.ApiResponse;
import com.gpstracker.dto.ErrorResponse;
import com.gpstracker.exception.BadRequestException;
import com.gpstracker.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST API controllers
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String INTERNAL_ERROR_MESSAGE = "An unexpected error occurred. Please try again later.";

    @ExceptionHandler(GeofenceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleGeofenceNotFoundException(GeofenceNotFoundException ex) {
        log.error("Geofence not found: {}", ex.getMessage());
        return createErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Geofence not found",
                ex.getMessage());
    }

    @ExceptionHandler(DeviceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleDeviceNotFoundException(DeviceNotFoundException ex) {
        log.error("Device not found: {}", ex.getMessage());
        return createErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Device not found",
                ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Invalid request: {}", ex.getMessage());
        return createErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid request parameters",
                ex.getMessage());
    }

    /**
     * Handles bad request exceptions (e.g. invalid input, constraint violations
     * from business logic)
     * 
     * @param ex The bad request exception
     * @return 400 error response
     */
    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBadRequestException(BadRequestException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return createErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad request",
                ex.getMessage());
    }

    /**
     * Handles resource not found exceptions
     * 
     * @param ex The resource not found exception
     * @return 404 error response
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return createErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Resource not found",
                ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal server error",
                "An unexpected error occurred. Please try again later.");
    }

    private Map<String, Object> createErrorResponse(int status, String error, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", status);
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        return errorResponse;
    }

    /**
     * Handles all custom business exceptions
     * 
     * @param ex The business exception
     * @return Appropriate error response
     */
    @ExceptionHandler(BaseBusinessException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleBusinessException(BaseBusinessException ex) {
        log.error("Business exception: {}", ex.getMessage(), ex);

        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.BAD_REQUEST;
        ErrorResponse errorResponse = buildErrorResponse(
                ex.getCode(),
                ex.getMessage(),
                ex.getDetails());

        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(errorResponse));
    }

    /**
     * Handles validation exceptions
     * 
     * @param ex The validation exception
     * @return Validation error response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.error("Validation error: {}", ex.getMessage(), ex);

        Map<String, Object> errors = new HashMap<>();
        Map<String, String> validationErrors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        errors.put("success", false);
        errors.put("message", "Validation failed");
        errors.put("data", validationErrors);
        errors.put("timestamp", LocalDateTime.now());

        return errors;
    }

    /**
     * Handles constraint violation exceptions
     * 
     * @param ex The constraint violation exception
     * @return Validation error response
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        log.debug("Constraint violation: {}", ex.getMessage());

        Map<String, List<String>> fieldErrors = ex.getConstraintViolations().stream()
                .collect(Collectors.groupingBy(
                        violation -> violation.getPropertyPath().toString(),
                        Collectors.mapping(violation -> violation.getMessage(), Collectors.toList())));

        ErrorResponse errorResponse = buildErrorResponse(
                "VALIDATION_ERROR",
                "Constraint violation",
                fieldErrors);

        return ApiResponse.error(errorResponse);
    }

    /**
     * Handles missing request parameters
     * 
     * @param ex The missing parameter exception
     * @return Error response
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<ErrorResponse> handleMissingParams(MissingServletRequestParameterException ex) {
        log.debug("Missing parameter: {}", ex.getMessage());

        Map<String, List<String>> details = new HashMap<>();
        details.put(ex.getParameterName(), List.of("Parameter is required"));

        ErrorResponse errorResponse = buildErrorResponse(
                "MISSING_PARAMETER",
                ex.getMessage(),
                details);

        return ApiResponse.error(errorResponse);
    }

    /**
     * Handles parameter type mismatch errors
     * 
     * @param ex The type mismatch exception
     * @return Error response
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.debug("Type mismatch: {}", ex.getMessage());

        String paramName = ex.getName();
        String requiredType = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName()
                : "unknown";

        Map<String, List<String>> details = new HashMap<>();
        details.put(paramName, List.of("Should be of type " + requiredType));

        ErrorResponse errorResponse = buildErrorResponse(
                "TYPE_MISMATCH",
                "Parameter type mismatch",
                details);

        return ApiResponse.error(errorResponse);
    }

    /**
     * Handles HTTP method not supported errors
     * 
     * @param ex The method not supported exception
     * @return Error response
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        log.debug("Method not allowed: {}", ex.getMessage());

        ErrorResponse errorResponse = buildErrorResponse(
                "METHOD_NOT_ALLOWED",
                "HTTP method not supported: " + ex.getMethod(),
                Map.of("supportedMethods",
                        ex.getSupportedMethods() != null ? List.of(ex.getSupportedMethods()) : List.of()));

        return ApiResponse.error(errorResponse);
    }

    /**
     * Handles unsupported media type errors
     * 
     * @param ex The media type not supported exception
     * @return Error response
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ApiResponse<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.debug("Media type not supported: {}", ex.getMessage());

        ErrorResponse errorResponse = buildErrorResponse(
                "UNSUPPORTED_MEDIA_TYPE",
                "Media type not supported",
                Map.of("contentType", List.of(ex.getContentType() != null ? ex.getContentType().toString() : "unknown"),
                        "supportedMediaTypes",
                        ex.getSupportedMediaTypes().stream().map(Object::toString).collect(Collectors.toList())));

        return ApiResponse.error(errorResponse);
    }

    /**
     * Handles errors parsing request body
     * 
     * @param ex The exception
     * @return Error response
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.debug("Message not readable: {}", ex.getMessage());

        ErrorResponse errorResponse = buildErrorResponse(
                "INVALID_REQUEST_BODY",
                "Invalid request body",
                null);

        return ApiResponse.error(errorResponse);
    }

    /**
     * Handles access denied errors
     * 
     * @param ex The access denied exception
     * @return Error response
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.debug("Access denied: {}", ex.getMessage());

        ErrorResponse errorResponse = buildErrorResponse(
                "ACCESS_DENIED",
                "Access denied",
                null);

        return ApiResponse.error(errorResponse);
    }

    /**
     * Handles data access errors
     * 
     * @param ex The data access exception
     * @return Error response
     */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<ErrorResponse> handleDataAccessException(DataAccessException ex) {
        log.error("Data access error: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = buildErrorResponse(
                "DATA_ACCESS_ERROR",
                INTERNAL_ERROR_MESSAGE,
                null);

        return ApiResponse.error(errorResponse);
    }

    /**
     * Handles file upload size errors
     * 
     * @param ex The max upload size exceeded exception
     * @return Error response
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ApiResponse<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.debug("Upload size exceeded: {}", ex.getMessage());

        ErrorResponse errorResponse = buildErrorResponse(
                "FILE_TOO_LARGE",
                "File upload size exceeded maximum allowed size",
                null);

        return ApiResponse.error(errorResponse);
    }

    /**
     * Creates a standardized error response
     */
    private ErrorResponse buildErrorResponse(String code, String message, Object details) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }
}