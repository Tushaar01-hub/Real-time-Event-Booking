package com.eventbooking.recommendation;

import com.eventbooking.recommendation.dto.RecommendationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interface for recommendation providers.
 * Allows for different implementations (rule-based, AI, etc.).
 */
public interface RecommendationProvider {

    /**
     * Get personalized recommendations for a user.
     * @param userId User ID
     * @param pageable Pagination
     * @return Page of recommendations
     */
    Page<RecommendationDto> getPersonalizedRecommendations(Long userId, Pageable pageable);

    /**
     * Get popular events (fallback).
     * @param pageable Pagination
     * @return Page of popular events
     */
    Page<RecommendationDto> getPopularEvents(Pageable pageable);

    /**
     * Get related events (fallback).
     * @param eventId Event ID
     * @param pageable Pagination
     * @return Page of related events
     */
    Page<RecommendationDto> getRelatedEvents(Long eventId, Pageable pageable);

    /**
     * Get upcoming events (fallback).
     * @param pageable Pagination
     * @return Page of upcoming events
     */
    Page<RecommendationDto> getUpcomingEvents(Pageable pageable);
}