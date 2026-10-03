package com.eventbooking.catalog.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Category entity.
 * Categories are read-heavy and rarely change.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Find category by slug (URL-friendly identifier).
     */
    Optional<Category> findBySlug(String slug);

    /**
     * Check if a category with the given name already exists (case-insensitive).
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Check if a category with the given slug already exists.
     */
    boolean existsBySlug(String slug);
}
