package com.eventbooking.catalog.category;

import com.eventbooking.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for Category operations.
 * Categories are read-heavy and rarely modified.
 */
@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    /**
     * Get all categories (typically for dropdown filters).
     */
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get a single category by ID.
     */
    public CategoryDto getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        return categoryMapper.toDto(category);
    }

    /**
     * Get a category by slug.
     */
    public CategoryDto getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", slug));
        return categoryMapper.toDto(category);
    }

    /**
     * Create a new category (admin only).
     */
    @Transactional
    public CategoryDto createCategory(CategoryDto dto) {
        // Check for duplicate name
        if (categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("Category with name '" + dto.getName() + "' already exists");
        }

        // Check for duplicate slug
        if (categoryRepository.existsBySlug(dto.getSlug())) {
            throw new IllegalArgumentException("Category with slug '" + dto.getSlug() + "' already exists");
        }

        Category category = categoryMapper.toEntity(dto);
        Category saved = categoryRepository.save(category);
        return categoryMapper.toDto(saved);
    }

    /**
     * Update an existing category (admin only).
     */
    @Transactional
    public CategoryDto updateCategory(Long id, CategoryDto dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        // Check for duplicate name (excluding current category)
        if (!category.getName().equalsIgnoreCase(dto.getName())
                && categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("Category with name '" + dto.getName() + "' already exists");
        }

        // Check for duplicate slug (excluding current category)
        if (!category.getSlug().equals(dto.getSlug())
                && categoryRepository.existsBySlug(dto.getSlug())) {
            throw new IllegalArgumentException("Category with slug '" + dto.getSlug() + "' already exists");
        }

        categoryMapper.updateEntity(dto, category);
        Category updated = categoryRepository.save(category);
        return categoryMapper.toDto(updated);
    }

    /**
     * Delete a category (admin only).
     * This will fail if any events reference this category due to FK constraint.
     */
    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category", id);
        }
        // If events reference this category, the database FK constraint will throw an exception
        categoryRepository.deleteById(id);
    }
}
