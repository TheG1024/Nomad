package com.gpstracker.controller;

import com.gpstracker.dto.ApiResponse;
import com.gpstracker.dto.CircleGeofenceDto;
import com.gpstracker.dto.GeoPointDto;
import com.gpstracker.dto.PaginatedResponse;
import com.gpstracker.dto.PaginationRequest;
import com.gpstracker.dto.PolygonGeofenceDto;
import com.gpstracker.dto.TimeRestrictionDto;
import com.gpstracker.exception.BadRequestException;
import com.gpstracker.exception.GeofenceNotFoundException;
import com.gpstracker.exception.ResourceNotFoundException;
import com.gpstracker.mapper.CircleGeofenceMapper;
import com.gpstracker.mapper.GeoPointMapper;
import com.gpstracker.mapper.PolygonGeofenceMapper;
import com.gpstracker.mapper.TimeRestrictionMapper;
import com.gpstracker.model.CircleGeofence;
import com.gpstracker.model.Geofence;
import com.gpstracker.model.GeoPoint;
import com.gpstracker.model.PolygonGeofence;
import com.gpstracker.model.TimeRestriction;
import com.gpstracker.service.GeofenceService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for managing geofences
 */
@Slf4j
@RestController
@RequestMapping("/api/geofences")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Validated
public class GeofenceController {

    private final GeofenceService geofenceService;
    private final CircleGeofenceMapper circleGeofenceMapper;
    private final PolygonGeofenceMapper polygonGeofenceMapper;
    private final GeoPointMapper geoPointMapper;
    private final TimeRestrictionMapper timeRestrictionMapper;

    /**
     * Create a new circular geofence
     */
    @PostMapping("/circle")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createCircleGeofence(
            @Valid @RequestBody CircleGeofenceDto geofenceDto) {
        log.debug("Creating circle geofence: {}", geofenceDto);
        try {
            CircleGeofence geofence = geofenceService.createCircleGeofence(geofenceDto);

            // Populate response matching API expectations
            Map<String, Object> data = new HashMap<>();
            data.put("id", geofence.getId());
            data.put("name", geofence.getName());
            data.put("description", geofence.getDescription());
            data.put("centerLatitude", geofence.getCenterLatitude());
            data.put("centerLongitude", geofence.getCenterLongitude());
            data.put("radiusMeters", geofence.getRadiusMeters());
            data.put("deviceId", geofence.getDeviceId());
            data.put("active", geofence.isActive());
            data.put("type", "circle");

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(data, "Geofence created successfully"));
        } catch (IllegalArgumentException e) {
            log.error("Failed to create geofence: {}", e.getMessage(), e);
            throw e; // Global exception handler will handle this
        } catch (Exception e) {
            log.error("Unexpected error creating geofence: {}", e.getMessage(), e);
            throw e; // Global exception handler will handle this
        }
    }

    /**
     * Create a polygon geofence
     * 
     * @param request The polygon geofence data
     * @return The created polygon geofence
     */
    @PostMapping("/polygon")
    public ResponseEntity<ApiResponse<PolygonGeofence>> createPolygonGeofence(
            @Valid @RequestBody PolygonGeofenceDto request) {
        try {
            if (request.getVertices() == null || request.getVertices().size() < 3) {
                throw new BadRequestException("Polygon must have at least 3 vertices");
            }

            // Convert DTO vertices to model GeoPoints
            List<GeoPoint> vertices = request.getVertices().stream()
                    .map(geoPointMapper::toEntity)
                    .collect(Collectors.toList());

            PolygonGeofence geofence = geofenceService.createPolygonGeofence(
                    request.getDeviceId(),
                    request.getName(),
                    request.getDescription(),
                    vertices,
                    request.getCategory(),
                    request.getAlertLevel());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(geofence, "Polygon geofence created successfully"));
        } catch (BadRequestException e) {
            log.error("Bad request for polygon geofence: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error creating polygon geofence: ", e);
            throw new BadRequestException("Failed to create polygon geofence: " + e.getMessage());
        }
    }

    /**
     * Set time restrictions for a geofence
     * 
     * @param id              The geofence ID
     * @param timeRestriction The time restriction data
     * @return The created/updated time restriction
     */
    @PostMapping("/{id}/time-restrictions")
    public ResponseEntity<ApiResponse<TimeRestrictionDto>> setTimeRestrictions(
            @PathVariable String id,
            @Valid @RequestBody TimeRestrictionDto timeRestriction) {

        try {
            // Validate time restriction
            if (timeRestriction.getStartTime() != null && timeRestriction.getEndTime() != null &&
                    timeRestriction.getStartTime().equals(timeRestriction.getEndTime())) {
                throw new BadRequestException("Start time and end time cannot be the same");
            }

            TimeRestriction restriction = geofenceService.createOrUpdateTimeRestriction(
                    id,
                    timeRestriction.getStartTime(),
                    timeRestriction.getEndTime());

            TimeRestrictionDto responseDto = timeRestrictionMapper.toDto(restriction);
            return ResponseEntity.ok(ApiResponse.success(responseDto, "Time restrictions set successfully"));
        } catch (ResourceNotFoundException e) {
            log.error("Geofence not found: {}", id);
            throw e;
        } catch (BadRequestException e) {
            log.error("Bad request for time restrictions: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error setting time restrictions: ", e);
            throw new BadRequestException("Failed to set time restrictions: " + e.getMessage());
        }
    }

    /**
     * Get time restrictions for a geofence
     * 
     * @param id The geofence ID
     * @return The time restriction if found
     */
    @GetMapping("/{id}/time-restrictions")
    public ResponseEntity<ApiResponse<TimeRestrictionDto>> getTimeRestrictions(@PathVariable String id) {
        try {
            TimeRestriction restriction = geofenceService.getTimeRestrictionByGeofenceId(id);
            if (restriction != null) {
                TimeRestrictionDto responseDto = timeRestrictionMapper.toDto(restriction);
                return ResponseEntity.ok(ApiResponse.success(responseDto));
            }
            throw new ResourceNotFoundException("TimeRestriction", id);
        } catch (ResourceNotFoundException e) {
            log.error("Time restriction not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error getting time restrictions: ", e);
            throw new BadRequestException("Failed to get time restrictions: " + e.getMessage());
        }
    }

    /**
     * Get all geofences for a device
     * 
     * @param deviceId The device ID
     * @return List of geofences for the device
     */
    @GetMapping("/device/{deviceId}")
    public ResponseEntity<ApiResponse<List<Geofence>>> getGeofencesForDevice(@PathVariable String deviceId) {
        try {
            List<Geofence> geofences = geofenceService.getGeofencesForDevice(deviceId);
            return ResponseEntity.ok(ApiResponse.success(geofences));
        } catch (Exception e) {
            log.error("Error retrieving geofences for device {}: {}", deviceId, e.getMessage());
            throw new BadRequestException("Failed to retrieve geofences: " + e.getMessage());
        }
    }

    /**
     * Get paginated geofences for a device with filtering options
     * 
     * @param deviceId         The device ID
     * @param page             Page number (0-based)
     * @param size             Page size (1-100)
     * @param sortBy           Field to sort by
     * @param ascending        Sort direction (true for ascending, false for
     *                         descending)
     * @param nameFilter       Filter by name (partial match)
     * @param categoryFilter   Filter by category (exact match)
     * @param alertLevelFilter Filter by alert level
     * @param activeFilter     Filter by active status
     * @param minLatitude      Minimum latitude for location filtering
     * @param maxLatitude      Maximum latitude for location filtering
     * @param minLongitude     Minimum longitude for location filtering
     * @param maxLongitude     Maximum longitude for location filtering
     * @return Paginated list of geofences
     */
    @GetMapping("/device/{deviceId}/paginated")
    public ResponseEntity<ApiResponse<PaginatedResponse<Geofence>>> getGeofencesForDevicePaginated(
            @PathVariable String deviceId,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "true") Boolean ascending,
            @RequestParam(required = false) String nameFilter,
            @RequestParam(required = false) String categoryFilter,
            @RequestParam(required = false) Integer alertLevelFilter,
            @RequestParam(required = false) Boolean activeFilter,
            @RequestParam(required = false) Double minLatitude,
            @RequestParam(required = false) Double maxLatitude,
            @RequestParam(required = false) Double minLongitude,
            @RequestParam(required = false) Double maxLongitude) {

        try {
            // Validate pagination parameters
            if (page < 0) {
                log.warn("Negative page number {} provided, defaulting to 0", page);
                page = 0;
            }

            // Ensure size is within bounds
            if (size < 1) {
                log.warn("Invalid page size {} provided, defaulting to 20", size);
                size = 20;
            } else if (size > 100) {
                log.warn("Page size {} exceeds maximum, limiting to 100", size);
                size = 100;
            }

            // Validate location bounds if provided
            if (minLatitude != null && maxLatitude != null && minLatitude > maxLatitude) {
                throw new BadRequestException("minLatitude cannot be greater than maxLatitude");
            }

            if (minLongitude != null && maxLongitude != null && minLongitude > maxLongitude) {
                throw new BadRequestException("minLongitude cannot be greater than maxLongitude");
            }

            // Create pagination request with filtering
            PaginationRequest paginationRequest = PaginationRequest.builder()
                    .page(page)
                    .size(size)
                    .sortBy(sortBy)
                    .ascending(ascending)
                    .nameFilter(nameFilter)
                    .categoryFilter(categoryFilter)
                    .alertLevelFilter(alertLevelFilter)
                    .activeFilter(activeFilter)
                    .minLatitude(minLatitude)
                    .maxLatitude(maxLatitude)
                    .minLongitude(minLongitude)
                    .maxLongitude(maxLongitude)
                    .build();

            PaginatedResponse<Geofence> paginatedResponse = geofenceService.getGeofencesForDevicePaginated(
                    deviceId, paginationRequest);

            return ResponseEntity.ok(ApiResponse.success(paginatedResponse));
        } catch (BadRequestException e) {
            log.error("Bad request for paginated geofences: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving paginated geofences: ", e);
            throw new BadRequestException("Failed to retrieve paginated geofences: " + e.getMessage());
        }
    }

    /**
     * Delete a geofence
     * 
     * @param id The geofence ID
     * @return Success response if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGeofence(@PathVariable Long id) {
        log.debug("Deleting geofence with ID: {}", id);
        try {
            geofenceService.deleteGeofence(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Geofence deleted successfully");
            return ResponseEntity.ok(response);
        } catch (GeofenceNotFoundException e) {
            log.error("Geofence not found for deletion: {}", e.getMessage());
            throw e; // Global exception handler will handle this
        } catch (Exception e) {
            log.error("Error deleting geofence: {}", e.getMessage(), e);
            throw e; // Global exception handler will handle this
        }
    }

    /**
     * Check if a point is within any geofences for a device
     * 
     * @param deviceId  The device ID
     * @param latitude  Latitude coordinate
     * @param longitude Longitude coordinate
     * @return Geofence check result
     */
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<GeofenceCheckResponse>> checkGeofences(
            @RequestParam String deviceId,
            @RequestParam double latitude,
            @RequestParam double longitude) {

        try {
            // Validate coordinates
            if (latitude < -90 || latitude > 90) {
                throw new BadRequestException("Latitude must be between -90 and 90 degrees");
            }

            if (longitude < -180 || longitude > 180) {
                throw new BadRequestException("Longitude must be between -180 and 180 degrees");
            }

            List<Geofence> triggeredGeofences = geofenceService.checkGeofences(deviceId, latitude, longitude);

            GeofenceCheckResponse response = new GeofenceCheckResponse();
            response.setInGeofence(!triggeredGeofences.isEmpty());
            response.setGeofences(triggeredGeofences);

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (BadRequestException e) {
            log.error("Bad request checking geofences: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error checking geofences: ", e);
            throw new BadRequestException("Failed to check geofences: " + e.getMessage());
        }
    }

    /**
     * Get a geofence by ID
     * 
     * @param id The geofence ID
     * @return The geofence if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getGeofenceById(@PathVariable Long id) {
        log.debug("Getting geofence with ID: {}", id);
        try {
            Object geofence = geofenceService.getGeofenceById(id);
            if (geofence == null) {
                throw new GeofenceNotFoundException(id);
            }

            return ResponseEntity.ok(ApiResponse.success(geofence));
        } catch (GeofenceNotFoundException e) {
            log.error("Geofence not found: {}", e.getMessage());
            throw e; // Global exception handler will handle this
        } catch (Exception e) {
            log.error("Error retrieving geofence: {}", e.getMessage(), e);
            throw e; // Global exception handler will handle this
        }
    }

    /**
     * Update geofence
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateGeofence(@PathVariable Long id, @RequestBody CircleGeofenceDto geofenceDto) {
        log.debug("Updating geofence ID {} with data: {}", id, geofenceDto);
        try {
            Object updatedGeofence = geofenceService.updateGeofence(id, geofenceDto);

            return ResponseEntity.ok(ApiResponse.success(updatedGeofence, "Geofence updated successfully"));
        } catch (GeofenceNotFoundException e) {
            log.error("Geofence not found for update: {}", e.getMessage());
            throw e; // Global exception handler will handle this
        } catch (Exception e) {
            log.error("Error updating geofence: {}", e.getMessage(), e);
            throw e; // Global exception handler will handle this
        }
    }

    /**
     * Search geofences by name
     */
    @GetMapping("/device/{deviceId}/search")
    public ResponseEntity<ApiResponse<List<Geofence>>> searchGeofences(
            @PathVariable String deviceId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category) {
        log.debug("Searching geofences for device {} with query {} and category {}", deviceId, query, category);
        try {
            List<Geofence> results = geofenceService.searchGeofences(deviceId, query, category);
            return ResponseEntity.ok(ApiResponse.success(results));
        } catch (Exception e) {
            log.error("Error searching geofences: ", e);
            throw new BadRequestException("Failed to search geofences: " + e.getMessage());
        }
    }

    /**
     * Response class for geofence check operation
     */
    @Data
    static class GeofenceCheckResponse {
        private boolean inGeofence;
        private List<Geofence> geofences;
    }
}