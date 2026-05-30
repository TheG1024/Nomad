package com.gpstracker.service;

import com.gpstracker.dto.CircleGeofenceDto;
import com.gpstracker.dto.PaginatedResponse;
import com.gpstracker.dto.PaginationRequest;
import com.gpstracker.dto.PolygonGeofenceDto;
import com.gpstracker.model.CircleGeofence;
import com.gpstracker.model.Geofence;
import com.gpstracker.model.GeoPoint;
import com.gpstracker.model.PolygonGeofence;
import com.gpstracker.model.TimeRestriction;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.Optional;

/**
 * Service for managing geofences
 */
public interface GeofenceService {

    /**
     * Creates a new circular geofence
     * 
     * @param geofenceDto The DTO containing geofence properties
     * @return The created geofence with metadata
     */
    CircleGeofence createCircleGeofence(CircleGeofenceDto geofenceDto);

    /**
     * Retrieves all geofences in the system
     * 
     * @return List of all geofences
     */
    List<Geofence> getAllGeofences();

    /**
     * Find a geofence by ID
     * 
     * @param id The geofence ID
     * @return The geofence if found
     */
    Optional<Geofence> findGeofenceById(Long id);

    /**
     * Delete a geofence by ID
     * 
     * @param id The geofence ID
     * @return true if deleted, false if not found
     */
    boolean deleteGeofence(Long id);

    /**
     * Update a circular geofence
     * 
     * @param id          The geofence ID
     * @param geofenceDto The updated properties
     * @return The updated geofence
     */
    CircleGeofence updateCircleGeofence(Long id, CircleGeofenceDto geofenceDto);

    /**
     * Create a circle geofence with detailed parameters
     */
    CircleGeofence createCircleGeofence(String deviceId, String name, String description,
            double latitude, double longitude, double radius,
            String alertLevel, String category, Map<String, Object> metadata);

    /**
     * Get a geofence by its ID
     * 
     * @param id Geofence ID
     * @return Geofence data if found, null otherwise
     */
    Object getGeofenceById(Long id);

    /**
     * Update an existing geofence
     * 
     * @param id          Geofence ID to update
     * @param geofenceDto Updated geofence data
     * @return Updated geofence data
     */
    Object updateGeofence(Long id, CircleGeofenceDto geofenceDto);

    /**
     * Create a new polygon geofence
     */
    PolygonGeofence createPolygonGeofence(String deviceId, String name, String description,
            List<GeoPoint> vertices, String category, int alertLevel);

    /**
     * Create a new polygon geofence from DTO
     */
    PolygonGeofence createPolygonGeofence(PolygonGeofenceDto dto);

    /**
     * Set time restrictions for an existing geofence
     */
    Geofence setTimeRestrictions(String geofenceId, LocalTime startTime, LocalTime endTime);

    /**
     * Create or update a TimeRestriction for a geofence
     */
    TimeRestriction createOrUpdateTimeRestriction(String geofenceId, LocalTime startTime, LocalTime endTime);

    /**
     * Get TimeRestriction by geofence ID
     */
    TimeRestriction getTimeRestrictionByGeofenceId(String geofenceId);

    /**
     * Check if a point is within any active geofence for a device
     */
    List<Geofence> checkGeofences(String deviceId, double latitude, double longitude);

    /**
     * Get a geofence by its ID
     */
    Geofence getGeofence(String id);

    /**
     * Get a geofence by its ID (alias for getGeofence)
     */
    default Geofence getGeofenceById(String id) {
        return getGeofence(id);
    }

    /**
     * Get all geofences for a device
     */
    List<Geofence> getGeofencesForDevice(String deviceId);

    /**
     * Get paginated geofences for a device
     * 
     * @param deviceId   Device ID to get geofences for
     * @param pagination Pagination parameters
     * @return Paginated list of geofences
     */
    PaginatedResponse<Geofence> getGeofencesForDevicePaginated(String deviceId, PaginationRequest pagination);

    /**
     * Delete a geofence
     */
    boolean deleteGeofence(String id);

    /**
     * Update an existing geofence
     */
    Geofence updateGeofence(Geofence geofence);

    /**
     * Search geofences by name or category for a device
     */
    List<Geofence> searchGeofences(String deviceId, String query, String category);
}