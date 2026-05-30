package com.gpstracker.mapper;

import com.gpstracker.dto.TimeRestrictionDto;
import com.gpstracker.model.TimeRestriction;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mapper for converting between TimeRestrictionDto and TimeRestriction entity
 */
@Component
public class TimeRestrictionMapper implements EntityMapper<TimeRestrictionDto, TimeRestriction> {
    
    @Override
    public TimeRestrictionDto toDto(TimeRestriction entity) {
        if (entity == null) {
            return null;
        }
        
        return TimeRestrictionDto.builder()
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .build();
    }
    
    @Override
    public TimeRestriction toEntity(TimeRestrictionDto dto) {
        if (dto == null) {
            return null;
        }
        
        return TimeRestriction.builder()
                .id(UUID.randomUUID().toString())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    @Override
    public TimeRestriction updateEntityFromDto(TimeRestrictionDto dto, TimeRestriction entity) {
        if (dto == null || entity == null) {
            return entity;
        }
        
        if (dto.getStartTime() != null) {
            entity.setStartTime(dto.getStartTime());
        }
        
        if (dto.getEndTime() != null) {
            entity.setEndTime(dto.getEndTime());
        }
        
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }
} 