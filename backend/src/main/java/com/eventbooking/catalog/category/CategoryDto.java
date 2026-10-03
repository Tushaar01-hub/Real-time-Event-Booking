package com.eventbooking.catalog.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for Category.
 * Used for both requests and responses.
 */
public class CategoryDto {

    private Long id;

    @NotBlank(message = "Category name is required")
    @Size(max = 60, message = "Category name must not exceed 60 characters")
    private String name;

    @NotBlank(message = "Category slug is required")
    @Size(max = 60, message = "Category slug must not exceed 60 characters")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
             message = "Slug must be lowercase alphanumeric with hyphens only")
    private String slug;

    // ---------------------------------------------------------------- constructors

    public CategoryDto() {
    }

    public CategoryDto(Long id, String name, String slug) {
        this.id = id;
        this.name = name;
        this.slug = slug;
    }

    // ---------------------------------------------------------------- getters/setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}
