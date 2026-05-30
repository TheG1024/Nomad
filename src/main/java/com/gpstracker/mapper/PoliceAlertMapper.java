package com.gpstracker.mapper;

import com.gpstracker.model.PoliceAlert;
import com.gpstracker.dto.PoliceAlertDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Mapper for PoliceAlert entities and DTOs.
 */
@org.mapstruct.Mapper(componentModel = "spring")
public interface PoliceAlertMapper {
    PoliceAlert toEntity(PoliceAlertDTO dto);
    PoliceAlertDTO toDTO(PoliceAlert entity);
    List<PoliceAlertDTO> toDTOList(List<PoliceAlert> entities);
    
    void updateEntityFromDTO(PoliceAlertDTO dto, @MappingTarget PoliceAlert entity);
}