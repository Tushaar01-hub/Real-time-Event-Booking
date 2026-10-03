package com.eventbooking.catalog.category;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Category entity representing an event category (e.g., Music, Sports, Theater).
 * Categories are relatively static and referenced by events.
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 60)
    @Column(nullable = false, unique = true)
    private String name;

    @NotBlank
    @Size(max = 60)
    @Column(nullable = false, unique = true)
    private String slug;

    // ---------------------------------------------------------------- constructors

    protected Category() {
        // JPA requires a no-arg constructor
    }

    public Category(String name, String slug) {
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
