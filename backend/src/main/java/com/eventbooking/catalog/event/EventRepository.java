package com.eventbooking.catalog.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository
        extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    /**
     * Find events by category IDs or locations.
     */
    @EntityGraph(attributePaths = {"category"})
    @Query("SELECT e FROM Event e WHERE e.category.id IN :categoryIds OR e.location IN :locations")
    Page<Event> findByCategoryIdsOrLocations(@Param("categoryIds") List<Long> categoryIds,
                                      @Param("locations") List<String> locations,
                                      Pageable pageable);

    /**
     * Find events by category ID.
     */
    @EntityGraph(attributePaths = {"category"})
    @Query("SELECT e FROM Event e WHERE e.category.id = :categoryId")
    Page<Event> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);
}
