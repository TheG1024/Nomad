package com.gpstracker.mapper;

import com.gpstracker.dto.TimeRestrictionDto;
import com.gpstracker.model.TimeRestriction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class TimeRestrictionMapperTest {

    private TimeRestrictionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TimeRestrictionMapper();
    }

    @Test
    void toDto_shouldReturnNullWhenEntityIsNull() {
        assertNull(mapper.toDto(null));
    }

    @Test
    void toDto_shouldMapAllFields() {
        // Arrange
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(17, 0);
        
        TimeRestriction entity = TimeRestriction.builder()
                .id("12345")
                .geofenceId("geofence123")
                .startTime(startTime)
                .endTime(endTime)
                .build();

        // Act
        TimeRestrictionDto dto = mapper.toDto(entity);

        // Assert
        assertNotNull(dto);
        assertEquals(entity.getStartTime(), dto.getStartTime());
        assertEquals(entity.getEndTime(), dto.getEndTime());
    }

    @Test
    void toEntity_shouldReturnNullWhenDtoIsNull() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    void toEntity_shouldMapAllFields() {
        // Arrange
        LocalTime startTime = LocalTime.of(22, 0);
        LocalTime endTime = LocalTime.of(6, 0);
        
        TimeRestrictionDto dto = TimeRestrictionDto.builder()
                .startTime(startTime)
                .endTime(endTime)
                .build();

        // Act
        TimeRestriction entity = mapper.toEntity(dto);

        // Assert
        assertNotNull(entity);
        assertNotNull(entity.getId()); // Should generate UUID
        assertEquals(dto.getStartTime(), entity.getStartTime());
        assertEquals(dto.getEndTime(), entity.getEndTime());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
    }

    @Test
    void updateEntityFromDto_shouldReturnEntityWhenDtoIsNull() {
        // Arrange
        TimeRestriction entity = TimeRestriction.builder()
                .id("12345")
                .geofenceId("geofence123")
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .build();

        // Act
        TimeRestriction result = mapper.updateEntityFromDto(null, entity);

        // Assert
        assertSame(entity, result);
    }

    @Test
    void updateEntityFromDto_shouldReturnEntityWhenEntityIsNull() {
        // Arrange
        TimeRestrictionDto dto = TimeRestrictionDto.builder()
                .startTime(LocalTime.of(22, 0))
                .endTime(LocalTime.of(6, 0))
                .build();

        // Act
        TimeRestriction result = mapper.updateEntityFromDto(dto, null);

        // Assert
        assertNull(result);
    }

    @Test
    void updateEntityFromDto_shouldUpdateAllFields() {
        // Arrange
        TimeRestriction entity = TimeRestriction.builder()
                .id("12345")
                .geofenceId("geofence123")
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .build();

        LocalTime newStartTime = LocalTime.of(20, 0);
        LocalTime newEndTime = LocalTime.of(8, 0);
        
        TimeRestrictionDto dto = TimeRestrictionDto.builder()
                .startTime(newStartTime)
                .endTime(newEndTime)
                .build();

        // Act
        TimeRestriction result = mapper.updateEntityFromDto(dto, entity);

        // Assert
        assertNotNull(result);
        assertEquals(dto.getStartTime(), result.getStartTime());
        assertEquals(dto.getEndTime(), result.getEndTime());
        assertNotNull(result.getUpdatedAt());
    }

    @Test
    void updateEntityFromDto_shouldOnlyUpdateNonNullFields() {
        // Arrange
        LocalTime originalStartTime = LocalTime.of(9, 0);
        LocalTime originalEndTime = LocalTime.of(17, 0);
        
        TimeRestriction entity = TimeRestriction.builder()
                .id("12345")
                .geofenceId("geofence123")
                .startTime(originalStartTime)
                .endTime(originalEndTime)
                .build();

        LocalTime newEndTime = LocalTime.of(18, 0);
        
        TimeRestrictionDto dto = TimeRestrictionDto.builder()
                .startTime(null)  // Null start time should not update the entity's start time
                .endTime(newEndTime)
                .build();

        // Act
        TimeRestriction result = mapper.updateEntityFromDto(dto, entity);

        // Assert
        assertNotNull(result);
        assertEquals(originalStartTime, result.getStartTime()); // Should not be updated
        assertEquals(newEndTime, result.getEndTime()); // Should be updated
    }
} 