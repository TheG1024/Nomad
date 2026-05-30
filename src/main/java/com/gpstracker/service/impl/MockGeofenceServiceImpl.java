package com.gpstracker.service.impl;

import com.gpstracker.dto.CircleGeofenceDto;
import com.gpstracker.dto.PaginatedResponse;
import com.gpstracker.dto.PaginationRequest;
import com.gpstracker.dto.PolygonGeofenceDto;
import com.gpstracker.model.CircleGeofence;
import com.gpstracker.model.Geofence;
import com.gpstracker.model.GeoPoint;
import com.gpstracker.model.PolygonGeofence;
import com.gpstracker.model.TimeRestriction;
import com.gpstracker.service.GeofenceService;
import com.gpstracker.exception.GeofenceNotFoundException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Mock implementation of GeofenceService for embedded mode
 * This implementation provides placeholder implementations
 * that work without Redis
 */
@Slf4j
@Service
@Profile("embedded | test")
public class MockGeofenceServiceImpl implements GeofenceService {

    private final Map<String, Geofence> geofences = new ConcurrentHashMap<>();
    private long nextId = 1;

    @Override
    public CircleGeofence createCircleGeofence(String deviceId, String name, String description,
            double latitude, double longitude, double radius,
            String alertLevel, String category, Map<String, Object> metadata) {
        CircleGeofence geofence = new CircleGeofence();
        String id = String.valueOf(nextId++);
        geofence.setId(id);
        geofence.setDeviceId(deviceId);
        geofence.setName(name);
        geofence.setDescription(description);
        geofence.setCenterLatitude(latitude);
        geofence.setCenterLongitude(longitude);
        geofence.setRadiusMeters(radius);
        geofence.setAlertLevel(alertLevel != null ? Integer.parseInt(alertLevel) : 1);
        geofence.setCategory(category);
        geofence.setActive(true);

        // Store in our mock repository
        geofences.put(id, geofence);

        return geofence;
    }

    @Override
    public CircleGeofence createCircleGeofence(CircleGeofenceDto dto) {
        return createCircleGeofence(
                dto.getDeviceId(),
                dto.getName(),
                dto.getDescription(),
                dto.getCenterLatitude(),
                dto.getCenterLongitude(),
                dto.getRadiusMeters(),
                dto.getAlertLevel() != null ? String.valueOf(dto.getAlertLevel()) : "1",
                dto.getCategory(),
                dto.getMetadata());
    }

    @Override
    public PolygonGeofence createPolygonGeofence(String deviceId, String name, String description,
            List<GeoPoint> vertices, String category, int alertLevel) {
        log.info("Creating polygon geofence for device {}", deviceId);
        PolygonGeofence geofence = new PolygonGeofence();
        String id = String.valueOf(nextId++);
        geofence.setId(id);
        geofence.setDeviceId(deviceId);
        geofence.setName(name);
        geofence.setDescription(description);
        geofence.setCategory(category);
        geofence.setAlertLevel(alertLevel);
        geofence.setVertices(vertices);
        geofence.setActive(true);
        geofence.setCreatedAt(LocalDateTime.now());
        geofence.setUpdatedAt(LocalDateTime.now());
        geofences.put(id, geofence);
        return geofence;
    }

    @Override
    public PolygonGeofence createPolygonGeofence(PolygonGeofenceDto dto) {
        log.info("Creating polygon geofence from DTO");
        PolygonGeofence geofence = new PolygonGeofence();
        String id = String.valueOf(nextId++);
        geofence.setId(id);
        geofence.setDeviceId(dto.getDeviceId());
        geofence.setName(dto.getName());
        geofence.setDescription(dto.getDescription());
        geofence.setCategory(dto.getCategory());
        geofence.setAlertLevel(dto.getAlertLevel());
        geofence.setActive(true);
        geofence.setCreatedAt(LocalDateTime.now());
        geofence.setUpdatedAt(LocalDateTime.now());
        geofences.put(id, geofence);
        return geofence;
    }

    @Override
    public Geofence setTimeRestrictions(String geofenceId, LocalTime startTime, LocalTime endTime) {
        log.info("Setting time restrictions for geofence {}", geofenceId);
        Geofence geofence = getGeofence(geofenceId);
        if (geofence != null) {
            geofence.setTimeRestricted(true);
            geofence.setActiveStartTime(startTime);
            geofence.setActiveEndTime(endTime);
            geofence.setUpdatedAt(LocalDateTime.now());
        }
        return geofence;
    }

    @Override
    public TimeRestriction createOrUpdateTimeRestriction(String geofenceId, LocalTime startTime, LocalTime endTime) {
        log.info("Creating time restriction for geofence {}", geofenceId);
        TimeRestriction restriction = new TimeRestriction();
        restriction.setId(UUID.randomUUID().toString());
        restriction.setGeofenceId(geofenceId);
        restriction.setStartTime(startTime);
        restriction.setEndTime(endTime);
        return restriction;
    }

    @Override
    public TimeRestriction getTimeRestrictionByGeofenceId(String geofenceId) {
        log.info("Getting time restriction for geofence {}", geofenceId);
        return null;
    }

    @Override
    public List<Geofence> checkGeofences(String deviceId, double latitude, double longitude) {
        log.info("Checking geofences for device {} at position {}, {}", deviceId, latitude, longitude);
        return Collections.emptyList();
    }

    @Override
    public Geofence getGeofence(String id) {
        log.info("Getting geofence with ID {}", id);
        return geofences.get(id);
    }

    @Override
    public List<Geofence> getGeofencesForDevice(String deviceId) {
        log.info("Getting geofences for device {}", deviceId);
        return geofences.values().stream()
                .filter(g -> deviceId.equals(g.getDeviceId()))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public PaginatedResponse<Geofence> getGeofencesForDevicePaginated(String deviceId, PaginationRequest pagination) {
        log.info("Getting paginated geofences for device {}", deviceId);
        List<Geofence> deviceGeofences = geofences.values().stream()
                .filter(g -> deviceId.equals(g.getDeviceId()))
                .collect(java.util.stream.Collectors.toList());

        int count = deviceGeofences.size();
        int page = pagination.getPage();
        int size = pagination.getSize();
        int from = Math.min(page * size, count);
        int to = Math.min((page + 1) * size, count);

        PaginatedResponse<Geofence> response = new PaginatedResponse<>();
        response.setContent(deviceGeofences.subList(from, to));
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements(count);
        response.setTotalPages((int) Math.ceil((double) count / size));
        response.setFirst(page == 0);
        response.setLast(to >= count);
        return response;
    }

    @Override
    public boolean deleteGeofence(String id) {
        log.info("Deleting geofence with ID {}", id);
        return geofences.remove(id) != null;
    }

    @Override
    public Geofence updateGeofence(Geofence geofence) {
        log.info("Updating geofence with ID {}", geofence.getId());
        geofence.setUpdatedAt(LocalDateTime.now());
        return geofence;
    }

    @Override
    public List<Geofence> getAllGeofences() {
        return new ArrayList<>(geofences.values());
    }

    @Override
    public Optional<Geofence> findGeofenceById(Long id) {
        return Optional.ofNullable(geofences.get(String.valueOf(id)));
    }

    @Override
    public CircleGeofence updateCircleGeofence(Long id, CircleGeofenceDto geofenceDto) {
        Optional<Geofence> existingGeofenceOpt = findGeofenceById(id);

        if (existingGeofenceOpt.isEmpty() || !(existingGeofenceOpt.get() instanceof CircleGeofence)) {
            throw new GeofenceNotFoundException("Circle geofence with ID " + id + " not found");
        }

        CircleGeofence existingGeofence = (CircleGeofence) existingGeofenceOpt.get();

        // Update the properties
        existingGeofence.setName(geofenceDto.getName());
        existingGeofence.setDescription(geofenceDto.getDescription());
        existingGeofence.setCenterLatitude(geofenceDto.getCenterLatitude());
        existingGeofence.setCenterLongitude(geofenceDto.getCenterLongitude());
        existingGeofence.setRadiusMeters(geofenceDto.getRadiusMeters());

        // In a mock implementation, we don't need to save, just return the updated
        // object
        return existingGeofence;
    }

    @Override
    public Object updateGeofence(Long id, CircleGeofenceDto geofenceDto) {
        log.info("Updating geofence with ID {}", id);
        return updateCircleGeofence(id, geofenceDto);
    }

    @Override
    public Object getGeofenceById(Long id) {
        log.info("Getting geofence by ID {}", id);
        return geofences.get(String.valueOf(id));
    }

    @Override
    public List<Geofence> searchGeofences(String deviceId, String query, String category) {
        log.info("Searching geofences for device {} with query {} and category {}", deviceId, query, category);
        return geofences.values().stream()
                .filter(g -> deviceId.equals(g.getDeviceId()))
                .filter(g -> query == null || query.isEmpty()
                        || (g.getName() != null && g.getName().toLowerCase().contains(query.toLowerCase())))
                .filter(g -> category == null || category.isEmpty() || category.equals(g.getCategory()))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public boolean deleteGeofence(Long id) {
        log.info("Deleting geofence with ID {}", id);
        return geofences.remove(String.valueOf(id)) != null;
    }
}