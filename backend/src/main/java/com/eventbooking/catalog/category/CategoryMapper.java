package com.eventbooking.catalog.category;

import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Category entity and CategoryDto.
 * Keeps entities isolated from the API layer.
 */
@Component
public class CategoryMapper {

    /**
     * Convert entity to DTO.
     */
    public CategoryDto toDto(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryDto(
            category.getId(),
            category.getName(),
            category.getSlug()
        );
    }

    /**
     * Convert DTO to new entity (for creation).
     */
    public Category toEntity(CategoryDto dto) {
        if (dto == null) {
            return null;
        }
        return new Category(dto.getName(), dto.getSlug());
    }

    /**
     * Update existing entity from DTO (for updates).
     */
    public void updateEntity(CategoryDto dto, Category category) {
        if (dto == null || category == null) {
            return;
        }
        category.setName(dto.getName());
        category.setSlug(dto.getSlug());
    }
}
