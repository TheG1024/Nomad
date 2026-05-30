package com.gpstracker.mapper;

import com.gpstracker.dto.GeoPointDto;
import com.gpstracker.model.GeoPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoPointMapperTest {

    private GeoPointMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new GeoPointMapper();
    }

    @Test
    void toDto_shouldReturnNullWhenEntityIsNull() {
        assertNull(mapper.toDto(null));
    }

    @Test
    void toDto_shouldMapAllFields() {
        // Arrange
        GeoPoint entity = GeoPoint.builder()
                .latitude(40.7128)
                .longitude(-74.0060)
                .label("New York")
                .build();

        // Act
        GeoPointDto dto = mapper.toDto(entity);

        // Assert
        assertNotNull(dto);
        assertEquals(entity.getLatitude(), dto.getLatitude());
        assertEquals(entity.getLongitude(), dto.getLongitude());
        assertEquals(entity.getLabel(), dto.getLabel());
    }

    @Test
    void toEntity_shouldReturnNullWhenDtoIsNull() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    void toEntity_shouldMapAllFields() {
        // Arrange
        GeoPointDto dto = GeoPointDto.builder()
                .latitude(51.5074)
                .longitude(-0.1278)
                .label("London")
                .build();

        // Act
        GeoPoint entity = mapper.toEntity(dto);

        // Assert
        assertNotNull(entity);
        assertEquals(dto.getLatitude(), entity.getLatitude());
        assertEquals(dto.getLongitude(), entity.getLongitude());
        assertEquals(dto.getLabel(), entity.getLabel());
    }

    @Test
    void updateEntityFromDto_shouldReturnEntityWhenDtoIsNull() {
        // Arrange
        GeoPoint entity = GeoPoint.builder()
                .latitude(48.8566)
                .longitude(2.3522)
                .label("Paris")
                .build();

        // Act
        GeoPoint result = mapper.updateEntityFromDto(null, entity);

        // Assert
        assertSame(entity, result);
    }

    @Test
    void updateEntityFromDto_shouldReturnEntityWhenEntityIsNull() {
        // Arrange
        GeoPointDto dto = GeoPointDto.builder()
                .latitude(35.6762)
                .longitude(139.6503)
                .label("Tokyo")
                .build();

        // Act
        GeoPoint result = mapper.updateEntityFromDto(dto, null);

        // Assert
        assertNull(result);
    }

    @Test
    void updateEntityFromDto_shouldUpdateAllFields() {
        // Arrange
        GeoPoint entity = GeoPoint.builder()
                .latitude(48.8566)
                .longitude(2.3522)
                .label("Paris")
                .build();

        GeoPointDto dto = GeoPointDto.builder()
                .latitude(35.6762)
                .longitude(139.6503)
                .label("Tokyo")
                .build();

        // Act
        GeoPoint result = mapper.updateEntityFromDto(dto, entity);

        // Assert
        assertNotNull(result);
        assertEquals(dto.getLatitude(), result.getLatitude());
        assertEquals(dto.getLongitude(), result.getLongitude());
        assertEquals(dto.getLabel(), result.getLabel());
    }

    @Test
    void updateEntityFromDto_shouldOnlyUpdateNonNullFields() {
        // Arrange
        GeoPoint entity = GeoPoint.builder()
                .latitude(48.8566)
                .longitude(2.3522)
                .label("Paris")
                .build();

        GeoPointDto dto = GeoPointDto.builder()
                .latitude(35.6762)
                .longitude(139.6503)
                .label(null)  // Null label should not update the entity's label
                .build();

        // Act
        GeoPoint result = mapper.updateEntityFromDto(dto, entity);

        // Assert
        assertNotNull(result);
        assertEquals(dto.getLatitude(), result.getLatitude());
        assertEquals(dto.getLongitude(), result.getLongitude());
        assertEquals("Paris", result.getLabel()); // Label should not be updated
    }
} 