package com.gpstracker.mapper;

import com.gpstracker.dto.GeoPointDto;
import com.gpstracker.dto.PolygonGeofenceDto;
import com.gpstracker.model.GeoPoint;
import com.gpstracker.model.PolygonGeofence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Mapper for converting between PolygonGeofenceDto and PolygonGeofence entity
 * Optimized for performance with large collections
 */
@Component
@RequiredArgsConstructor
public class PolygonGeofenceMapper implements EntityMapper<PolygonGeofenceDto, PolygonGeofence> {
    
    private final GeoPointMapper geoPointMapper;
    
    @Override
    public PolygonGeofenceDto toDto(PolygonGeofence entity) {
        if (entity == null) {
            return null;
        }
        
        List<GeoPointDto> vertices = null;
        if (entity.getVertices() != null && !entity.getVertices().isEmpty()) {
            // Pre-allocate collection with exact size for better performance
            List<GeoPoint> sourceVertices = entity.getVertices();
            vertices = new ArrayList<>(sourceVertices.size());
            
            // Manual iteration instead of stream for better performance
            for (GeoPoint point : sourceVertices) {
                if (point != null) {
                    vertices.add(geoPointMapper.toDto(point));
                }
            }
        }
        
        return PolygonGeofenceDto.builder()
                .deviceId(entity.getDeviceId())
                .name(entity.getName())
                .description(entity.getDescription())
                .vertices(vertices)
                .category(entity.getCategory())
                .alertLevel(entity.getAlertLevel())
                .color(entity.getColor())
                .build();
    }
    
    @Override
    public PolygonGeofence toEntity(PolygonGeofenceDto dto) {
        if (dto == null) {
            return null;
        }
        
        List<GeoPoint> vertices = null;
        if (dto.getVertices() != null && !dto.getVertices().isEmpty()) {
            // Pre-allocate collection with exact size for better performance
            List<GeoPointDto> sourceVertices = dto.getVertices();
            vertices = new ArrayList<>(sourceVertices.size());
            
            // Manual iteration instead of stream for better performance
            for (GeoPointDto point : sourceVertices) {
                if (point != null) {
                    vertices.add(geoPointMapper.toEntity(point));
                }
            }
        }
        
        PolygonGeofence geofence = PolygonGeofence.builder()
                .deviceId(dto.getDeviceId())
                .name(dto.getName())
                .description(dto.getDescription())
                .vertices(vertices)
                .category(dto.getCategory())
                .alertLevel(dto.getAlertLevel())
                .color(dto.getColor())
                .active(true)
                .timeRestricted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
                
        geofence.generateId();
        return geofence;
    }
    
    @Override
    public List<PolygonGeofenceDto> toDtoList(List<PolygonGeofence> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Pre-allocate collection with exact size for better performance
        List<PolygonGeofenceDto> dtos = new ArrayList<>(entities.size());
        
        // Manual iteration instead of stream for better performance
        for (PolygonGeofence entity : entities) {
            PolygonGeofenceDto dto = toDto(entity);
            if (dto != null) {
                dtos.add(dto);
            }
        }
        
        return dtos;
    }
    
    @Override
    public List<PolygonGeofence> toEntityList(List<PolygonGeofenceDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Pre-allocate collection with exact size for better performance
        List<PolygonGeofence> entities = new ArrayList<>(dtos.size());
        
        // Manual iteration instead of stream for better performance
        for (PolygonGeofenceDto dto : dtos) {
            PolygonGeofence entity = toEntity(dto);
            if (entity != null) {
                entities.add(entity);
            }
        }
        
        return entities;
    }
    
    @Override
    public PolygonGeofence updateEntityFromDto(PolygonGeofenceDto dto, PolygonGeofence entity) {
        if (dto == null || entity == null) {
            return entity;
        }
        
        if (dto.getDeviceId() != null) {
            entity.setDeviceId(dto.getDeviceId());
        }
        
        if (dto.getName() != null) {
            entity.setName(dto.getName());
        }
        
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        
        if (dto.getVertices() != null && !dto.getVertices().isEmpty()) {
            // Pre-allocate collection with exact size for better performance
            List<GeoPointDto> sourceVertices = dto.getVertices();
            List<GeoPoint> vertices = new ArrayList<>(sourceVertices.size());
            
            // Manual iteration instead of stream for better performance
            for (GeoPointDto point : sourceVertices) {
                if (point != null) {
                    vertices.add(geoPointMapper.toEntity(point));
                }
            }
            
            entity.setVertices(vertices);
        }
        
        if (dto.getCategory() != null) {
            entity.setCategory(dto.getCategory());
        }
        
        if (dto.getAlertLevel() != null) {
            entity.setAlertLevel(dto.getAlertLevel());
        }
        
        if (dto.getColor() != null) {
            entity.setColor(dto.getColor());
        }
        
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }
} 