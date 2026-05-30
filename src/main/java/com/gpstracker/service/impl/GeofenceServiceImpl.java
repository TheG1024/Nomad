package com.gpstracker.service.impl;

import com.gpstracker.config.RedisConfig;
import com.gpstracker.dto.CircleGeofenceDto;
import com.gpstracker.dto.PaginatedResponse;
import com.gpstracker.dto.PaginationRequest;
import com.gpstracker.dto.PolygonGeofenceDto;
import com.gpstracker.exception.GeofenceNotFoundException;
import com.gpstracker.exception.ResourceNotFoundException;
import com.gpstracker.mapper.CircleGeofenceMapper;
import com.gpstracker.mapper.PolygonGeofenceMapper;
import com.gpstracker.model.CircleGeofence;
import com.gpstracker.model.Geofence;
import com.gpstracker.model.GeoPoint;
import com.gpstracker.model.PolygonGeofence;
import com.gpstracker.model.TimeRestriction;
import com.gpstracker.service.GeofenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Redis-based implementation of the GeofenceService interface.
 * Only active when the embedded profile is not active.
 */
@Slf4j
@Service
@Profile("!embedded & !test")
public class GeofenceServiceImpl implements GeofenceService {

    private static final String GEOFENCE_KEY_PREFIX = "geofence:";
    private static final String DEVICE_GEOFENCES_KEY_PREFIX = "device:geofences:";
    private static final String TIME_RESTRICTION_KEY_PREFIX = "time-restriction:";

    private final Map<String, List<Geofence>> activeGeofences = new ConcurrentHashMap<>();
    private final Map<String, Geofence> geofences = new ConcurrentHashMap<>();

    @Autowired
    private RedisConfig redisConfig;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CircleGeofenceMapper circleGeofenceMapper;

    @Autowired
    private PolygonGeofenceMapper polygonGeofenceMapper;

    @Override
    public CircleGeofence createCircleGeofence(String deviceId, String name, String description,
            double latitude, double longitude, double radius,
            String alertLevel, String category, Map<String, Object> metadata) {
        log.debug("Creating circle geofence for device {} at {}, {} with radius {} meters",
                deviceId, latitude, longitude, radius);

        CircleGeofenceDto dto = CircleGeofenceDto.builder()
                .deviceId(deviceId)
                .name(name)
                .description(description)
                .alertLevel(alertLevel != null ? Integer.parseInt(alertLevel) : 1)
                .centerLatitude(latitude)
                .centerLongitude(longitude)
                .radiusMeters(radius)
                .category(category)
                .metadata(metadata)
                .build();

        return createCircleGeofence(dto);
    }

    @Override
    public CircleGeofence createCircleGeofence(CircleGeofenceDto dto) {
        // Implementation for creating CircleGeofence from DTO
        log.debug("Creating CircleGeofence from DTO: {}", dto);
        log.debug("DTO details - name: {}, latitude: {}, longitude: {}, radiusMeters: {}",
                dto.getName(), dto.getCenterLatitude(), dto.getCenterLongitude(), dto.getRadiusMeters());

        CircleGeofence geofence = circleGeofenceMapper.toEntity(dto);
        log.debug("Converted to entity: {}", geofence);

        // Generate ID and save
        String id = UUID.randomUUID().toString();
        geofence.setId(id);
        geofences.put(id, geofence);

        return geofence;
    }

    @Override
    public PolygonGeofence createPolygonGeofence(String deviceId, String name, String description,
            List<GeoPoint> vertices, String category, int alertLevel) {
        PolygonGeofenceDto dto = PolygonGeofenceDto.builder()
                .deviceId(deviceId)
                .name(name)
                .description(description)
                .category(category)
                .alertLevel(alertLevel)
                .build();

        PolygonGeofence geofence = polygonGeofenceMapper.toEntity(dto);
        geofence.setVertices(vertices);
        saveGeofence(geofence);
        return geofence;
    }

    @Override
    public PolygonGeofence createPolygonGeofence(PolygonGeofenceDto dto) {
        PolygonGeofence geofence = polygonGeofenceMapper.toEntity(dto);
        saveGeofence(geofence);
        return geofence;
    }

    @Override
    public TimeRestriction createOrUpdateTimeRestriction(String geofenceId, LocalTime startTime, LocalTime endTime) {
        // First check if the geofence exists
        Geofence geofence = getGeofence(geofenceId);
        if (geofence == null) {
            throw new ResourceNotFoundException("Geofence", geofenceId);
        }

        // Set the geofence as time restricted
        geofence.setTimeRestricted(true);
        geofence.setActiveStartTime(startTime);
        geofence.setActiveEndTime(endTime);
        geofence.setUpdatedAt(LocalDateTime.now());
        saveGeofence(geofence);

        // Create or update the time restriction entity
        String restrictionKey = TIME_RESTRICTION_KEY_PREFIX + geofenceId;
        TimeRestriction timeRestriction = null;

        try {
            timeRestriction = (TimeRestriction) redisTemplate.opsForValue().get(restrictionKey);
        } catch (RedisConnectionFailureException e) {
            // Use in-memory fallback
            timeRestriction = (TimeRestriction) RedisConfig.getFromMemory(restrictionKey);
        }

        if (timeRestriction == null) {
            // Create new time restriction
            timeRestriction = TimeRestriction.builder()
                    .id(UUID.randomUUID().toString())
                    .geofenceId(geofenceId)
                    .startTime(startTime)
                    .endTime(endTime)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        } else {
            // Update existing time restriction
            timeRestriction.setStartTime(startTime);
            timeRestriction.setEndTime(endTime);
            timeRestriction.setUpdatedAt(LocalDateTime.now());
        }

        // Save the time restriction
        try {
            redisTemplate.opsForValue().set(restrictionKey, timeRestriction);
        } catch (RedisConnectionFailureException e) {
            // Use in-memory fallback
            RedisConfig.setInMemory(restrictionKey, timeRestriction);
        }

        log.info("Saved time restriction for geofence: {}", geofenceId);

        return timeRestriction;
    }

    @Override
    public Geofence setTimeRestrictions(String geofenceId, LocalTime startTime, LocalTime endTime) {
        createOrUpdateTimeRestriction(geofenceId, startTime, endTime);
        return getGeofence(geofenceId);
    }

    @Override
    public List<Geofence> checkGeofences(String deviceId, double latitude, double longitude) {
        List<Geofence> triggeredGeofences = new ArrayList<>();
        LocalTime currentTime = LocalTime.now();

        List<Geofence> geofences = getGeofencesForDevice(deviceId);

        for (Geofence geofence : geofences) {
            if (geofence.isActiveAtTime(currentTime) && geofence.containsPoint(latitude, longitude)) {
                triggeredGeofences.add(geofence);
            }
        }

        return triggeredGeofences;
    }

    /**
     * Save geofence to Redis and update in-memory cache
     */
    private void saveGeofence(Geofence geofence) {
        String key = GEOFENCE_KEY_PREFIX + geofence.getId();

        try {
            redisTemplate.opsForValue().set(key, geofence);

            // Add to device geofences set
            String deviceGeofencesKey = DEVICE_GEOFENCES_KEY_PREFIX + geofence.getDeviceId();
            redisTemplate.opsForSet().add(deviceGeofencesKey, geofence.getId());
        } catch (RedisConnectionFailureException e) {
            // Use in-memory fallback
            RedisConfig.setInMemory(key, geofence);

            // Add to device geofences set in memory
            String deviceGeofencesKey = DEVICE_GEOFENCES_KEY_PREFIX + geofence.getDeviceId();

            @SuppressWarnings("unchecked")
            List<String> deviceGeofenceIds = (List<String>) RedisConfig.getFromMemory(deviceGeofencesKey);
            if (deviceGeofenceIds == null) {
                deviceGeofenceIds = new ArrayList<>();
            }
            if (!deviceGeofenceIds.contains(geofence.getId())) {
                deviceGeofenceIds.add(geofence.getId());
                RedisConfig.setInMemory(deviceGeofencesKey, deviceGeofenceIds);
            }
        }

        // Update in-memory cache
        activeGeofences.computeIfAbsent(geofence.getDeviceId(), k -> new ArrayList<>());

        // Remove existing geofence with same ID if exists
        activeGeofences.get(geofence.getDeviceId()).removeIf(g -> g.getId().equals(geofence.getId()));

        // Add updated geofence
        if (geofence.isActive()) {
            activeGeofences.get(geofence.getDeviceId()).add(geofence);
        }

        log.info("Saved geofence: {}", geofence.getId());
    }

    @Override
    @Cacheable(value = "geofence", key = "#id")
    public Geofence getGeofence(String id) {
        String key = GEOFENCE_KEY_PREFIX + id;
        Geofence geofence = null;

        try {
            geofence = (Geofence) redisTemplate.opsForValue().get(key);
        } catch (RedisConnectionFailureException e) {
            // Use in-memory fallback
            geofence = (Geofence) RedisConfig.getFromMemory(key);
        }

        if (geofence == null) {
            log.warn("Geofence not found with id: {}", id);
        }

        return geofence;
    }

    @Override
    @Cacheable(value = "deviceGeofences", key = "#deviceId")
    public List<Geofence> getGeofencesForDevice(String deviceId) {
        String deviceGeofencesKey = DEVICE_GEOFENCES_KEY_PREFIX + deviceId;

        // Check if we have cached geofences
        if (activeGeofences.containsKey(deviceId)) {
            return activeGeofences.get(deviceId);
        }

        // Otherwise load from Redis or in-memory storage
        List<Geofence> geofences = new ArrayList<>();

        try {
            List<Object> geofenceIds = new ArrayList<>();
            try {
                geofenceIds = redisTemplate.opsForSet().members(deviceGeofencesKey)
                        .stream().collect(Collectors.toList());
            } catch (RedisConnectionFailureException e) {
                // Use in-memory fallback
                @SuppressWarnings("unchecked")
                List<String> ids = (List<String>) RedisConfig.getFromMemory(deviceGeofencesKey);
                if (ids != null) {
                    geofenceIds = new ArrayList<>(ids);
                }
            }

            if (geofenceIds != null && !geofenceIds.isEmpty()) {
                for (Object geofenceId : geofenceIds) {
                    String key = GEOFENCE_KEY_PREFIX + geofenceId.toString();
                    Geofence geofence = null;

                    try {
                        geofence = (Geofence) redisTemplate.opsForValue().get(key);
                    } catch (RedisConnectionFailureException e) {
                        // Use in-memory fallback
                        geofence = (Geofence) RedisConfig.getFromMemory(key);
                    }

                    if (geofence != null && geofence.isActive()) {
                        geofences.add(geofence);
                    }
                }
            }

            // Cache the result
            activeGeofences.put(deviceId, geofences);

        } catch (Exception e) {
            log.error("Error retrieving geofences for device {}: {}", deviceId, e.getMessage());
        }

        return geofences;
    }

    @Override
    @CacheEvict(value = { "geofence", "deviceGeofences" }, allEntries = true)
    public boolean deleteGeofence(String id) {
        Geofence geofence = getGeofence(id);
        if (geofence != null) {
            String key = GEOFENCE_KEY_PREFIX + id;

            try {
                redisTemplate.delete(key);

                // Remove from device geofences set
                String deviceGeofencesKey = DEVICE_GEOFENCES_KEY_PREFIX + geofence.getDeviceId();
                redisTemplate.opsForSet().remove(deviceGeofencesKey, id);
            } catch (RedisConnectionFailureException e) {
                // Use in-memory fallback
                RedisConfig.setInMemory(key, null);

                // Remove from device geofences set in memory
                String deviceGeofencesKey = DEVICE_GEOFENCES_KEY_PREFIX + geofence.getDeviceId();
                @SuppressWarnings("unchecked")
                List<String> deviceGeofenceIds = (List<String>) RedisConfig.getFromMemory(deviceGeofencesKey);
                if (deviceGeofenceIds != null) {
                    deviceGeofenceIds.remove(id);
                    RedisConfig.setInMemory(deviceGeofencesKey, deviceGeofenceIds);
                }
            }

            // Update in-memory cache
            if (activeGeofences.containsKey(geofence.getDeviceId())) {
                activeGeofences.get(geofence.getDeviceId()).removeIf(g -> g.getId().equals(id));
            }

            log.info("Deleted geofence: {}", id);
            return true;
        }
        return false;
    }

    @Override
    @CacheEvict(value = { "geofence", "deviceGeofences" }, allEntries = true)
    public Geofence updateGeofence(Geofence geofence) {
        if (geofence == null || geofence.getId() == null) {
            throw new IllegalArgumentException("Geofence ID must not be null");
        }

        Geofence existingGeofence = getGeofence(geofence.getId());
        if (existingGeofence == null) {
            throw new ResourceNotFoundException("Geofence", geofence.getId());
        }

        geofence.setUpdatedAt(LocalDateTime.now());
        saveGeofence(geofence);

        log.info("Updated geofence: {}", geofence.getId());
        return geofence;
    }

    @Override
    @Cacheable(value = "timeRestriction", key = "#geofenceId")
    public TimeRestriction getTimeRestrictionByGeofenceId(String geofenceId) {
        // First check if the geofence exists
        Geofence geofence = getGeofence(geofenceId);
        if (geofence == null || !geofence.isTimeRestricted()) {
            return null;
        }

        // Get the time restriction from Redis or in-memory storage
        String restrictionKey = TIME_RESTRICTION_KEY_PREFIX + geofenceId;
        TimeRestriction timeRestriction = null;

        try {
            timeRestriction = (TimeRestriction) redisTemplate.opsForValue().get(restrictionKey);
        } catch (RedisConnectionFailureException e) {
            // Use in-memory fallback
            timeRestriction = (TimeRestriction) RedisConfig.getFromMemory(restrictionKey);
        }

        if (timeRestriction == null) {
            // If the explicit time restriction doesn't exist but the geofence has time
            // restrictions,
            // create and return a TimeRestriction from the geofence's fields
            timeRestriction = TimeRestriction.builder()
                    .id(UUID.randomUUID().toString())
                    .geofenceId(geofenceId)
                    .startTime(geofence.getActiveStartTime())
                    .endTime(geofence.getActiveEndTime())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // Save it for future use
            try {
                redisTemplate.opsForValue().set(restrictionKey, timeRestriction);
            } catch (RedisConnectionFailureException e) {
                // Use in-memory fallback
                RedisConfig.setInMemory(restrictionKey, timeRestriction);
            }
        }

        return timeRestriction;
    }

    @Override
    @Cacheable(value = "filteredGeofences", key = "{#deviceId, #pagination.page, #pagination.size, #pagination.sortBy, #pagination.ascending, #pagination.nameFilter, #pagination.categoryFilter, #pagination.alertLevelFilter, #pagination.activeFilter, #pagination.minLatitude, #pagination.maxLatitude, #pagination.minLongitude, #pagination.maxLongitude}")
    public PaginatedResponse<Geofence> getGeofencesForDevicePaginated(String deviceId, PaginationRequest pagination) {
        // Get all geofences for device
        List<Geofence> allGeofences = getGeofencesForDevice(deviceId);

        // Apply filters
        allGeofences = filterGeofences(allGeofences, pagination);

        // Sort if needed
        if (pagination.getSortBy() != null && !pagination.getSortBy().isEmpty()) {
            allGeofences = sortGeofences(allGeofences, pagination.getSortBy(), pagination.getAscending());
        } else {
            // Default sort by created date
            allGeofences.sort(Comparator.comparing(Geofence::getCreatedAt).reversed());
        }

        // Calculate pagination details
        int totalElements = allGeofences.size();
        int totalPages = (int) Math.ceil((double) totalElements / pagination.getSize());

        // Apply pagination
        int fromIndex = pagination.getPage() * pagination.getSize();
        if (fromIndex >= totalElements) {
            // If page is out of bounds, return empty list
            return PaginatedResponse.<Geofence>builder()
                    .content(Collections.emptyList())
                    .page(pagination.getPage())
                    .size(pagination.getSize())
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .first(pagination.getPage() == 0)
                    .last(true)
                    .build();
        }

        int toIndex = Math.min(fromIndex + pagination.getSize(), totalElements);
        List<Geofence> paginatedGeofences = allGeofences.subList(fromIndex, toIndex);

        // Build response
        return PaginatedResponse.<Geofence>builder()
                .content(paginatedGeofences)
                .page(pagination.getPage())
                .size(pagination.getSize())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(pagination.getPage() == 0)
                .last(pagination.getPage() >= totalPages - 1)
                .build();
    }

    /**
     * Filter geofences based on pagination request filters
     */
    private List<Geofence> filterGeofences(List<Geofence> geofences, PaginationRequest pagination) {
        return geofences.stream()
                .filter(geofence -> {
                    // Filter by name if provided
                    if (pagination.getNameFilter() != null && !pagination.getNameFilter().isEmpty()) {
                        if (geofence.getName() == null ||
                                !geofence.getName().toLowerCase().contains(pagination.getNameFilter().toLowerCase())) {
                            return false;
                        }
                    }

                    // Filter by category if provided
                    if (pagination.getCategoryFilter() != null && !pagination.getCategoryFilter().isEmpty()) {
                        if (geofence.getCategory() == null ||
                                !geofence.getCategory().equals(pagination.getCategoryFilter())) {
                            return false;
                        }
                    }

                    // Filter by alert level if provided
                    if (pagination.getAlertLevelFilter() != null) {
                        if (geofence.getAlertLevel() != pagination.getAlertLevelFilter()) {
                            return false;
                        }
                    }

                    // Filter by active status if provided
                    if (pagination.getActiveFilter() != null) {
                        if (geofence.isActive() != pagination.getActiveFilter()) {
                            return false;
                        }
                    }

                    // Filter by location bounds if provided
                    if (isLocationFilterApplied(pagination)) {
                        return isGeofenceInBounds(geofence, pagination);
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * Check if any location filters are applied
     */
    private boolean isLocationFilterApplied(PaginationRequest pagination) {
        return pagination.getMinLatitude() != null || pagination.getMaxLatitude() != null ||
                pagination.getMinLongitude() != null || pagination.getMaxLongitude() != null;
    }

    /**
     * Check if a geofence is within the specified bounds
     */
    private boolean isGeofenceInBounds(Geofence geofence, PaginationRequest pagination) {
        // For circle geofences
        if (geofence instanceof CircleGeofence) {
            CircleGeofence circle = (CircleGeofence) geofence;

            // Check if center is within bounds
            return isPointInBounds(
                    circle.getCenterLatitude(),
                    circle.getCenterLongitude(),
                    pagination);
        }

        // For polygon geofences, check if any vertex is within bounds
        if (geofence instanceof PolygonGeofence) {
            PolygonGeofence polygon = (PolygonGeofence) geofence;

            if (polygon.getVertices() != null) {
                for (GeoPoint vertex : polygon.getVertices()) {
                    if (isPointInBounds(vertex.getLatitude(), vertex.getLongitude(), pagination)) {
                        return true;
                    }
                }
            }
            return false;
        }

        // For other types, default to true
        return true;
    }

    /**
     * Check if a point is within the specified bounds
     */
    private boolean isPointInBounds(Double latitude, Double longitude, PaginationRequest pagination) {
        if (latitude == null || longitude == null) {
            return false;
        }

        // Check latitude bounds if specified
        if (pagination.getMinLatitude() != null && latitude < pagination.getMinLatitude()) {
            return false;
        }
        if (pagination.getMaxLatitude() != null && latitude > pagination.getMaxLatitude()) {
            return false;
        }

        // Check longitude bounds if specified
        if (pagination.getMinLongitude() != null && longitude < pagination.getMinLongitude()) {
            return false;
        }
        if (pagination.getMaxLongitude() != null && longitude > pagination.getMaxLongitude()) {
            return false;
        }

        return true;
    }

    /**
     * Sort geofences by the given field
     */
    private List<Geofence> sortGeofences(List<Geofence> geofences, String sortBy, boolean ascending) {
        Comparator<Geofence> comparator = null;

        switch (sortBy.toLowerCase()) {
            case "name":
                comparator = Comparator.comparing(Geofence::getName);
                break;
            case "createdat":
                comparator = Comparator.comparing(Geofence::getCreatedAt);
                break;
            case "updatedat":
                comparator = Comparator.comparing(Geofence::getUpdatedAt);
                break;
            case "category":
                comparator = Comparator.comparing(g -> g.getCategory() != null ? g.getCategory() : "");
                break;
            case "alertlevel":
                comparator = Comparator.comparing(Geofence::getAlertLevel);
                break;
            default:
                comparator = Comparator.comparing(Geofence::getCreatedAt);
        }

        if (!ascending) {
            comparator = comparator.reversed();
        }

        return geofences.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    @Override
    public List<Geofence> getAllGeofences() {
        log.debug("Getting all geofences");
        return new ArrayList<>(geofences.values());
    }

    @Override
    public Optional<Geofence> findGeofenceById(Long id) {
        log.debug("Finding geofence by ID: {}", id);
        return Optional.ofNullable(geofences.get(id.toString()));
    }

    @Override
    public Object getGeofenceById(Long id) {
        log.debug("Getting geofence by ID: {}", id);
        return geofences.get(id.toString());
    }

    @Override
    public Object updateGeofence(Long id, CircleGeofenceDto geofenceDto) {
        log.debug("Updating geofence with ID: {}", id);
        return updateCircleGeofence(id, geofenceDto);
    }

    @Override
    public boolean deleteGeofence(Long id) {
        log.debug("Deleting geofence with ID: {}", id);
        return geofences.remove(id.toString()) != null;
    }

    @Override
    public CircleGeofence updateCircleGeofence(Long id, CircleGeofenceDto geofenceDto) {
        log.debug("Updating circle geofence with ID: {}", id);
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

        // Save the updated geofence
        geofences.put(id.toString(), existingGeofence);
        return existingGeofence;
    }

    @Override
    public List<Geofence> searchGeofences(String deviceId, String query, String category) {
        log.debug("Searching geofences for device {} with query {} and category {}", deviceId, query, category);
        List<Geofence> allGeofences = getGeofencesForDevice(deviceId);

        return allGeofences.stream()
                .filter(g -> query == null || query.isEmpty()
                        || (g.getName() != null && g.getName().toLowerCase().contains(query.toLowerCase())))
                .filter(g -> category == null || category.isEmpty() || category.equals(g.getCategory()))
                .collect(Collectors.toList());
    }
}