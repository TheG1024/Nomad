package com.gpstracker.mapper;

import com.gpstracker.dto.GeoPointDto;
import com.gpstracker.model.GeoPoint;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between GeoPointDto and GeoPoint entity
 */
@Component
public class GeoPointMapper implements EntityMapper<GeoPointDto, GeoPoint> {
    
    @Override
    public GeoPointDto toDto(GeoPoint entity) {
        if (entity == null) {
            return null;
        }
        
        return GeoPointDto.builder()
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .label(entity.getLabel())
                .build();
    }
    
    @Override
    public GeoPoint toEntity(GeoPointDto dto) {
        if (dto == null) {
            return null;
        }
        
        return GeoPoint.builder()
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .label(dto.getLabel())
                .build();
    }
    
    @Override
    public GeoPoint updateEntityFromDto(GeoPointDto dto, GeoPoint entity) {
        if (dto == null || entity == null) {
            return entity;
        }
        
        entity.setLatitude(dto.getLatitude());
        entity.setLongitude(dto.getLongitude());
        
        if (dto.getLabel() != null) {
            entity.setLabel(dto.getLabel());
        }
        
        return entity;
    }
} 