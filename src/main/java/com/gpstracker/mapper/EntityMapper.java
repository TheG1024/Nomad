package com.gpstracker.mapper;

import java.util.List;

/**
 * Generic mapper interface for converting between DTOs and domain models.
 * <p>
 * This interface defines the standard methods that should be implemented by all mapper classes 
 * in the application. It provides a consistent approach to transforming objects between the 
 * presentation and domain layers.
 * <p>
 * Implementation notes:
 * <ul>
 *   <li>All implementations should be thread-safe</li>
 *   <li>Implementations should gracefully handle null inputs</li>
 *   <li>When transforming collections, null values within the collection should be filtered out</li>
 *   <li>Default implementations are provided for list conversions, but may be overridden for performance optimization</li>
 * </ul>
 *
 * @param <D> DTO type - The Data Transfer Object used in the presentation layer
 * @param <E> Entity type - The domain model entity used in the service layer
 */
public interface EntityMapper<D, E> {
    
    /**
     * Converts a domain entity to its corresponding DTO representation.
     * <p>
     * This method handles the transformation of a single entity instance to a DTO.
     * It should intelligently map all relevant properties from the source entity
     * to the destination DTO.
     * <p>
     * Edge cases:
     * <ul>
     *   <li>If the input entity is null, the method should return null</li>
     *   <li>Required fields in the DTO should be handled gracefully when the source is missing data</li>
     *   <li>Complex nested objects should be properly transformed using their respective mappers</li>
     * </ul>
     *
     * @param entity The domain entity to convert (may be null)
     * @return The DTO representation of the entity, or null if the input is null
     */
    D toDto(E entity);
    
    /**
     * Converts a DTO to its corresponding domain entity representation.
     * <p>
     * This method handles the transformation of a single DTO instance to an entity.
     * It should intelligently map all relevant properties from the source DTO
     * to the destination entity.
     * <p>
     * Edge cases:
     * <ul>
     *   <li>If the input DTO is null, the method should return null</li>
     *   <li>Required fields in the entity should be handled gracefully when the source is missing data</li>
     *   <li>IDs and other identifying fields should be properly transferred when present</li>
     *   <li>Default values should be provided for essential entity fields missing from the DTO</li>
     *   <li>Complex nested objects should be properly transformed using their respective mappers</li>
     * </ul>
     *
     * @param dto The DTO to convert (may be null)
     * @return The domain entity representation of the DTO, or null if the input is null
     */
    E toEntity(D dto);
    
    /**
     * Converts a list of domain entities to a list of corresponding DTOs.
     * <p>
     * This default implementation iterates through each entity in the input list,
     * converts it using the {@link #toDto(Object)} method, and collects the results.
     * <p>
     * Edge cases:
     * <ul>
     *   <li>If the input list is null or empty, an empty list is returned (never null)</li>
     *   <li>Null values within the input list are filtered out</li>
     * </ul>
     * <p>
     * Performance considerations:
     * <ul>
     *   <li>This default implementation uses streams which are convenient but may not be 
     *       optimal for very large collections</li>
     *   <li>Consider overriding this method with a more efficient implementation for 
     *       mappers that will handle large collections</li>
     * </ul>
     *
     * @param entities List of domain entities to convert (may be null or empty)
     * @return List of DTOs corresponding to the input entities, or an empty list if input is null/empty
     */
    default List<D> toDtoList(List<E> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream()
                .filter(entity -> entity != null)
                .map(this::toDto)
                .toList();
    }
    
    /**
     * Converts a list of DTOs to a list of corresponding domain entities.
     * <p>
     * This default implementation iterates through each DTO in the input list,
     * converts it using the {@link #toEntity(Object)} method, and collects the results.
     * <p>
     * Edge cases:
     * <ul>
     *   <li>If the input list is null or empty, an empty list is returned (never null)</li>
     *   <li>Null values within the input list are filtered out</li>
     * </ul>
     * <p>
     * Performance considerations:
     * <ul>
     *   <li>This default implementation uses streams which are convenient but may not be 
     *       optimal for very large collections</li>
     *   <li>Consider overriding this method with a more efficient implementation for 
     *       mappers that will handle large collections</li>
     * </ul>
     *
     * @param dtos List of DTOs to convert (may be null or empty)
     * @return List of domain entities corresponding to the input DTOs, or an empty list if input is null/empty
     */
    default List<E> toEntityList(List<D> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return List.of();
        }
        return dtos.stream()
                .filter(dto -> dto != null)
                .map(this::toEntity)
                .toList();
    }
    
    /**
     * Updates an existing domain entity with data from a DTO.
     * <p>
     * This method selectively updates the properties of an existing entity based on
     * the data provided in the DTO. Unlike {@link #toEntity(Object)}, which creates
     * a new entity instance, this method modifies an existing entity.
     * <p>
     * Implementation guidelines:
     * <ul>
     *   <li>Only non-null properties in the DTO should overwrite the entity's properties</li>
     *   <li>Collections in the entity should be updated carefully to avoid unintended data loss</li>
     *   <li>Immutable properties (e.g., ID, creation timestamp) should typically not be modified</li>
     *   <li>Entity's timestamps or version fields should be updated appropriately</li>
     * </ul>
     * <p>
     * Edge cases:
     * <ul>
     *   <li>If the DTO input is null, the entity should be returned unchanged</li>
     *   <li>If the entity input is null, implementations may either return null or throw an exception</li>
     *   <li>Nested objects should be updated using their respective mappers</li>
     * </ul>
     *
     * @param dto The DTO containing the data to update with (may be null)
     * @param entity The existing entity to update (may be null depending on implementation)
     * @return The updated entity instance, or the unchanged entity if the DTO is null
     */
    E updateEntityFromDto(D dto, E entity);
} 