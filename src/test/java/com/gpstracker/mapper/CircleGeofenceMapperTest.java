package com.gpstracker.mapper;

import com.gpstracker.dto.CircleGeofenceDto;
import com.gpstracker.model.CircleGeofence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CircleGeofenceMapperTest {

    private CircleGeofenceMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CircleGeofenceMapper();
    }

    @Test
    void toDto_shouldReturnNullWhenEntityIsNull() {
        assertNull(mapper.toDto(null));
    }

    @Test
    void toDto_shouldMapAllFields() {
        // Arrange
        CircleGeofence entity = CircleGeofence.builder()
                .id("circle123")
                .deviceId("device123")
                .name("Work Geofence")
                .description("My office location")
                .centerLatitude(40.7128)
                .centerLongitude(-74.0060)
                .radiusMeters(500.0)
                .category("work")
                .alertLevel(2)
                .color("#FF5733")
                .active(true)
                .timeRestricted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Act
        CircleGeofenceDto dto = mapper.toDto(entity);

        // Assert
        assertNotNull(dto);
        assertEquals(entity.getDeviceId(), dto.getDeviceId());
        assertEquals(entity.getName(), dto.getName());
        assertEquals(entity.getDescription(), dto.getDescription());
        assertEquals(entity.getCenterLatitude(), dto.getCenterLatitude());
        assertEquals(entity.getCenterLongitude(), dto.getCenterLongitude());
        assertEquals(entity.getRadiusMeters(), dto.getRadiusMeters());
        assertEquals(entity.getCategory(), dto.getCategory());
        assertEquals(entity.getAlertLevel(), dto.getAlertLevel());
        assertEquals(entity.getColor(), dto.getColor());
    }

    @Test
    void toEntity_shouldReturnNullWhenDtoIsNull() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    void toEntity_shouldMapAllFields() {
        // Arrange
        CircleGeofenceDto dto = CircleGeofenceDto.builder()
                .deviceId("device456")
                .name("Home Geofence")
                .description("My home location")
                .centerLatitude(34.0522)
                .centerLongitude(-118.2437)
                .radiusMeters(100.0)
                .category("home")
                .alertLevel(1)
                .color("#33FF57")
                .build();

        // Act
        CircleGeofence entity = mapper.toEntity(dto);

        // Assert
        assertNotNull(entity);
        assertNotNull(entity.getId()); // Should generate ID
        assertEquals(dto.getDeviceId(), entity.getDeviceId());
        assertEquals(dto.getName(), entity.getName());
        assertEquals(dto.getDescription(), entity.getDescription());
        assertEquals(dto.getCenterLatitude(), entity.getCenterLatitude());
        assertEquals(dto.getCenterLongitude(), entity.getCenterLongitude());
        assertEquals(dto.getRadiusMeters(), entity.getRadiusMeters());
        assertEquals(dto.getCategory(), entity.getCategory());
        assertEquals(dto.getAlertLevel(), entity.getAlertLevel());
        assertEquals(dto.getColor(), entity.getColor());
        assertTrue(entity.isActive());
        assertFalse(entity.isTimeRestricted());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
    }

    @Test
    void updateEntityFromDto_shouldReturnEntityWhenDtoIsNull() {
        // Arrange
        CircleGeofence entity = CircleGeofence.builder()
                .id("circle123")
                .deviceId("device123")
                .name("Work Geofence")
                .description("My office location")
                .centerLatitude(40.7128)
                .centerLongitude(-74.0060)
                .radiusMeters(500.0)
                .category("work")
                .alertLevel(2)
                .color("#FF5733")
                .active(true)
                .timeRestricted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Act
        CircleGeofence result = mapper.updateEntityFromDto(null, entity);

        // Assert
        assertSame(entity, result);
    }

    @Test
    void updateEntityFromDto_shouldReturnEntityWhenEntityIsNull() {
        // Arrange
        CircleGeofenceDto dto = CircleGeofenceDto.builder()
                .deviceId("device456")
                .name("Home Geofence")
                .description("My home location")
                .centerLatitude(34.0522)
                .centerLongitude(-118.2437)
                .radiusMeters(100.0)
                .category("home")
                .alertLevel(1)
                .color("#33FF57")
                .build();

        // Act
        CircleGeofence result = mapper.updateEntityFromDto(dto, null);

        // Assert
        assertNull(result);
    }

    @Test
    void updateEntityFromDto_shouldUpdateAllFields() {
        // Arrange
        LocalDateTime originalCreatedAt = LocalDateTime.now().minusDays(1);
        LocalDateTime originalUpdatedAt = LocalDateTime.now().minusHours(1);
        
        CircleGeofence entity = CircleGeofence.builder()
                .id("circle123")
                .deviceId("device123")
                .name("Work Geofence")
                .description("My office location")
                .centerLatitude(40.7128)
                .centerLongitude(-74.0060)
                .radiusMeters(500.0)
                .category("work")
                .alertLevel(2)
                .color("#FF5733")
                .active(true)
                .timeRestricted(false)
                .createdAt(originalCreatedAt)
                .updatedAt(originalUpdatedAt)
                .build();

        CircleGeofenceDto dto = CircleGeofenceDto.builder()
                .deviceId("device456")
                .name("Home Geofence")
                .description("My home location")
                .centerLatitude(34.0522)
                .centerLongitude(-118.2437)
                .radiusMeters(100.0)
                .category("home")
                .alertLevel(1)
                .color("#33FF57")
                .build();

        // Act
        CircleGeofence result = mapper.updateEntityFromDto(dto, entity);

        // Assert
        assertNotNull(result);
        assertEquals("circle123", result.getId()); // ID should not change
        assertEquals(dto.getDeviceId(), result.getDeviceId());
        assertEquals(dto.getName(), result.getName());
        assertEquals(dto.getDescription(), result.getDescription());
        assertEquals(dto.getCenterLatitude(), result.getCenterLatitude());
        assertEquals(dto.getCenterLongitude(), result.getCenterLongitude());
        assertEquals(dto.getRadiusMeters(), result.getRadiusMeters());
        assertEquals(dto.getCategory(), result.getCategory());
        assertEquals(dto.getAlertLevel(), result.getAlertLevel());
        assertEquals(dto.getColor(), result.getColor());
        assertEquals(originalCreatedAt, result.getCreatedAt()); // Created at should not change
        assertNotEquals(originalUpdatedAt, result.getUpdatedAt()); // Updated at should change
    }

    @Test
    void updateEntityFromDto_shouldOnlyUpdateNonNullFields() {
        // Arrange
        CircleGeofence entity = CircleGeofence.builder()
                .id("circle123")
                .deviceId("device123")
                .name("Work Geofence")
                .description("My office location")
                .centerLatitude(40.7128)
                .centerLongitude(-74.0060)
                .radiusMeters(500.0)
                .category("work")
                .alertLevel(2)
                .color("#FF5733")
                .build();

        CircleGeofenceDto dto = CircleGeofenceDto.builder()
                .name("Updated Work Geofence")
                .centerLatitude(40.7130) // Slightly different
                .radiusMeters(600.0)
                .build();

        // Act
        CircleGeofence result = mapper.updateEntityFromDto(dto, entity);

        // Assert
        assertNotNull(result);
        assertEquals("circle123", result.getId());
        assertEquals("device123", result.getDeviceId()); // Should not change
        assertEquals("Updated Work Geofence", result.getName()); // Should update
        assertEquals("My office location", result.getDescription()); // Should not change
        assertEquals(40.7130, result.getCenterLatitude()); // Should update
        assertEquals(-74.0060, result.getCenterLongitude()); // Should not change
        assertEquals(600.0, result.getRadiusMeters()); // Should update
        assertEquals("work", result.getCategory()); // Should not change
        assertEquals(2, result.getAlertLevel()); // Should not change
        assertEquals("#FF5733", result.getColor()); // Should not change
    }
} 