package com.gpstracker.mapper;

import com.gpstracker.dto.CircleGeofenceDto;
import com.gpstracker.model.CircleGeofence;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper class for transforming between CircleGeofence entity and DTO objects
 */
@Component
public class CircleGeofenceMapper {

    /**
     * Convert from DTO to entity
     */
    public CircleGeofence toEntity(CircleGeofenceDto dto) {
        if (dto == null) {
            return null;
        }

        CircleGeofence entity = CircleGeofence.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .deviceId(dto.getDeviceId())
                .centerLatitude(dto.getCenterLatitude())
                .centerLongitude(dto.getCenterLongitude())
                .radiusMeters(dto.getRadiusMeters())
                .category(dto.getCategory())
                .alertLevel(dto.getAlertLevel() != null ? dto.getAlertLevel() : 1)
                .color(dto.getColor())
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        entity.generateId();
        return entity;
    }

    /**
     * Convert from entity to DTO
     */
    public CircleGeofenceDto toDto(CircleGeofence entity) {
        if (entity == null) {
            return null;
        }

        return CircleGeofenceDto.builder()
                .deviceId(entity.getDeviceId())
                .name(entity.getName())
                .description(entity.getDescription())
                .centerLatitude(entity.getCenterLatitude())
                .centerLongitude(entity.getCenterLongitude())
                .radiusMeters(entity.getRadiusMeters())
                .category(entity.getCategory())
                .alertLevel(entity.getAlertLevel())
                .color(entity.getColor())
                .metadata(null) // Could map if needed
                .build();
    }

    /**
     * Update an existing entity from DTO
     */
    public CircleGeofence updateEntityFromDto(CircleGeofenceDto dto, CircleGeofence entity) {
        if (entity == null) {
            return null;
        }
        if (dto == null) {
            return entity;
        }

        if (dto.getDeviceId() != null)
            entity.setDeviceId(dto.getDeviceId());
        if (dto.getName() != null)
            entity.setName(dto.getName());
        if (dto.getDescription() != null)
            entity.setDescription(dto.getDescription());
        if (dto.getCenterLatitude() != null)
            entity.setCenterLatitude(dto.getCenterLatitude());
        if (dto.getCenterLongitude() != null)
            entity.setCenterLongitude(dto.getCenterLongitude());
        if (dto.getRadiusMeters() != null)
            entity.setRadiusMeters(dto.getRadiusMeters());
        if (dto.getCategory() != null)
            entity.setCategory(dto.getCategory());
        if (dto.getAlertLevel() != null)
            entity.setAlertLevel(dto.getAlertLevel());
        if (dto.getColor() != null)
            entity.setColor(dto.getColor());

        entity.setUpdatedAt(LocalDateTime.now());

        return entity;
    }
}