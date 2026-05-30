package com.gpstracker.mapper;

import com.gpstracker.dto.GeoPointDto;
import com.gpstracker.dto.PolygonGeofenceDto;
import com.gpstracker.model.GeoPoint;
import com.gpstracker.model.PolygonGeofence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolygonGeofenceMapperTest {

    @Mock(lenient = true)
    private GeoPointMapper geoPointMapper;

    @InjectMocks
    private PolygonGeofenceMapper mapper;

    private GeoPoint point1, point2, point3;
    private GeoPointDto pointDto1, pointDto2, pointDto3;
    private List<GeoPoint> vertices;
    private List<GeoPointDto> verticesDto;

    @BeforeEach
    void setUp() {
        // Setup test data
        point1 = GeoPoint.builder().latitude(40.7128).longitude(-74.0060).label("Vertex 1").build();
        point2 = GeoPoint.builder().latitude(34.0522).longitude(-118.2437).label("Vertex 2").build();
        point3 = GeoPoint.builder().latitude(41.8781).longitude(-87.6298).label("Vertex 3").build();
        vertices = Arrays.asList(point1, point2, point3);

        pointDto1 = GeoPointDto.builder().latitude(40.7128).longitude(-74.0060).label("Vertex 1").build();
        pointDto2 = GeoPointDto.builder().latitude(34.0522).longitude(-118.2437).label("Vertex 2").build();
        pointDto3 = GeoPointDto.builder().latitude(41.8781).longitude(-87.6298).label("Vertex 3").build();
        verticesDto = Arrays.asList(pointDto1, pointDto2, pointDto3);

        // Setup mock behavior
        when(geoPointMapper.toDto(point1)).thenReturn(pointDto1);
        when(geoPointMapper.toDto(point2)).thenReturn(pointDto2);
        when(geoPointMapper.toDto(point3)).thenReturn(pointDto3);
        
        when(geoPointMapper.toEntity(pointDto1)).thenReturn(point1);
        when(geoPointMapper.toEntity(pointDto2)).thenReturn(point2);
        when(geoPointMapper.toEntity(pointDto3)).thenReturn(point3);
    }

    @Test
    void toDto_shouldReturnNullWhenEntityIsNull() {
        assertNull(mapper.toDto(null));
        verifyNoInteractions(geoPointMapper);
    }

    @Test
    void toDto_shouldMapAllFields() {
        // Arrange
        PolygonGeofence entity = PolygonGeofence.builder()
                .id("polygon123")
                .deviceId("device123")
                .name("Triangle Geofence")
                .description("A triangular area")
                .vertices(vertices)
                .category("restricted")
                .alertLevel(3)
                .color("#3366FF")
                .active(true)
                .timeRestricted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Act
        PolygonGeofenceDto dto = mapper.toDto(entity);

        // Assert
        assertNotNull(dto);
        assertEquals(entity.getDeviceId(), dto.getDeviceId());
        assertEquals(entity.getName(), dto.getName());
        assertEquals(entity.getDescription(), dto.getDescription());
        assertEquals(entity.getCategory(), dto.getCategory());
        assertEquals(entity.getAlertLevel(), dto.getAlertLevel());
        assertEquals(entity.getColor(), dto.getColor());
        
        assertNotNull(dto.getVertices());
        assertEquals(3, dto.getVertices().size());
        
        verify(geoPointMapper, times(3)).toDto(any(GeoPoint.class));
        verify(geoPointMapper).toDto(point1);
        verify(geoPointMapper).toDto(point2);
        verify(geoPointMapper).toDto(point3);
    }

    @Test
    void toDto_shouldHandleNullVertices() {
        // Arrange
        PolygonGeofence entity = PolygonGeofence.builder()
                .id("polygon123")
                .deviceId("device123")
                .name("Empty Geofence")
                .vertices(null)
                .build();

        // Act
        PolygonGeofenceDto dto = mapper.toDto(entity);

        // Assert
        assertNotNull(dto);
        assertNull(dto.getVertices());
        verifyNoInteractions(geoPointMapper);
    }

    @Test
    void toEntity_shouldReturnNullWhenDtoIsNull() {
        assertNull(mapper.toEntity(null));
        verifyNoInteractions(geoPointMapper);
    }

    @Test
    void toEntity_shouldMapAllFields() {
        // Arrange
        PolygonGeofenceDto dto = PolygonGeofenceDto.builder()
                .deviceId("device456")
                .name("School Zone")
                .description("A school safety zone")
                .vertices(verticesDto)
                .category("school")
                .alertLevel(2)
                .color("#FF99CC")
                .build();

        // Act
        PolygonGeofence entity = mapper.toEntity(dto);

        // Assert
        assertNotNull(entity);
        assertNotNull(entity.getId()); // Should generate ID
        assertEquals(dto.getDeviceId(), entity.getDeviceId());
        assertEquals(dto.getName(), entity.getName());
        assertEquals(dto.getDescription(), entity.getDescription());
        assertEquals(dto.getCategory(), entity.getCategory());
        assertEquals(dto.getAlertLevel(), entity.getAlertLevel());
        assertEquals(dto.getColor(), entity.getColor());
        assertTrue(entity.isActive());
        assertFalse(entity.isTimeRestricted());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        
        assertNotNull(entity.getVertices());
        assertEquals(3, entity.getVertices().size());
        
        verify(geoPointMapper, times(3)).toEntity(any(GeoPointDto.class));
        verify(geoPointMapper).toEntity(pointDto1);
        verify(geoPointMapper).toEntity(pointDto2);
        verify(geoPointMapper).toEntity(pointDto3);
    }

    @Test
    void toEntity_shouldHandleNullVertices() {
        // Arrange
        PolygonGeofenceDto dto = PolygonGeofenceDto.builder()
                .deviceId("device456")
                .name("Empty Geofence")
                .vertices(null)
                .alertLevel(1)
                .build();

        // Act
        PolygonGeofence entity = mapper.toEntity(dto);

        // Assert
        assertNotNull(entity);
        assertNull(entity.getVertices());
        assertEquals(1, entity.getAlertLevel());
        verifyNoInteractions(geoPointMapper);
    }

    @Test
    void updateEntityFromDto_shouldReturnEntityWhenDtoIsNull() {
        // Arrange
        PolygonGeofence entity = PolygonGeofence.builder()
                .id("polygon123")
                .name("Original Name")
                .build();

        // Act
        PolygonGeofence result = mapper.updateEntityFromDto(null, entity);

        // Assert
        assertSame(entity, result);
        verifyNoInteractions(geoPointMapper);
    }

    @Test
    void updateEntityFromDto_shouldReturnEntityWhenEntityIsNull() {
        // Arrange
        PolygonGeofenceDto dto = PolygonGeofenceDto.builder()
                .name("New Name")
                .build();

        // Act
        PolygonGeofence result = mapper.updateEntityFromDto(dto, null);

        // Assert
        assertNull(result);
        verifyNoInteractions(geoPointMapper);
    }

    @Test
    void updateEntityFromDto_shouldUpdateAllFields() {
        // Arrange
        LocalDateTime originalCreatedAt = LocalDateTime.now().minusDays(1);
        LocalDateTime originalUpdatedAt = LocalDateTime.now().minusHours(1);
        
        PolygonGeofence entity = PolygonGeofence.builder()
                .id("polygon123")
                .deviceId("device123")
                .name("Triangle Geofence")
                .description("A triangular area")
                .vertices(vertices.subList(0, 2)) // Only first 2 vertices
                .category("restricted")
                .alertLevel(3)
                .color("#3366FF")
                .active(true)
                .timeRestricted(false)
                .createdAt(originalCreatedAt)
                .updatedAt(originalUpdatedAt)
                .build();

        PolygonGeofenceDto dto = PolygonGeofenceDto.builder()
                .deviceId("device456")
                .name("Updated Triangle")
                .description("An updated triangular area")
                .vertices(verticesDto) // All 3 vertices
                .category("school")
                .alertLevel(2)
                .color("#FF99CC")
                .build();

        // Act
        PolygonGeofence result = mapper.updateEntityFromDto(dto, entity);

        // Assert
        assertNotNull(result);
        assertEquals("polygon123", result.getId()); // ID should not change
        assertEquals(dto.getDeviceId(), result.getDeviceId());
        assertEquals(dto.getName(), result.getName());
        assertEquals(dto.getDescription(), result.getDescription());
        assertEquals(dto.getCategory(), result.getCategory());
        assertEquals(dto.getAlertLevel(), result.getAlertLevel());
        assertEquals(dto.getColor(), result.getColor());
        assertEquals(originalCreatedAt, result.getCreatedAt()); // Created at should not change
        assertNotEquals(originalUpdatedAt, result.getUpdatedAt()); // Updated at should change
        
        assertNotNull(result.getVertices());
        assertEquals(3, result.getVertices().size()); // Should now have 3 vertices
        
        verify(geoPointMapper, times(3)).toEntity(any(GeoPointDto.class));
    }

    @Test
    void updateEntityFromDto_shouldOnlyUpdateNonNullFields() {
        // Arrange
        PolygonGeofence entity = PolygonGeofence.builder()
                .id("polygon123")
                .deviceId("device123")
                .name("Triangle Geofence")
                .description("A triangular area")
                .vertices(vertices)
                .category("restricted")
                .alertLevel(3)
                .color("#3366FF")
                .build();

        PolygonGeofenceDto dto = PolygonGeofenceDto.builder()
                .name("Updated Triangle")
                .alertLevel(2)
                .build();

        // Act
        PolygonGeofence result = mapper.updateEntityFromDto(dto, entity);

        // Assert
        assertNotNull(result);
        assertEquals("polygon123", result.getId());
        assertEquals("device123", result.getDeviceId()); // Should not change
        assertEquals("Updated Triangle", result.getName()); // Should update
        assertEquals("A triangular area", result.getDescription()); // Should not change
        assertEquals("restricted", result.getCategory()); // Should not change
        assertEquals(2, result.getAlertLevel()); // Should update
        assertEquals("#3366FF", result.getColor()); // Should not change
        assertSame(vertices, result.getVertices()); // Vertices should not change
        
        verifyNoInteractions(geoPointMapper); // Should not convert vertices
    }

    @Test
    void updateEntityFromDto_shouldUpdateVerticesWhenProvided() {
        // Arrange
        PolygonGeofence entity = PolygonGeofence.builder()
                .id("polygon123")
                .name("Triangle Geofence")
                .vertices(vertices.subList(0, 2)) // Only first 2 vertices
                .build();

        PolygonGeofenceDto dto = PolygonGeofenceDto.builder()
                .vertices(verticesDto) // All 3 vertices
                .build();

        // Act
        PolygonGeofence result = mapper.updateEntityFromDto(dto, entity);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getVertices());
        assertEquals(3, result.getVertices().size()); // Should now have 3 vertices
        
        verify(geoPointMapper, times(3)).toEntity(any(GeoPointDto.class));
    }
} 