package com.eventbooking.recommendation;

import com.eventbooking.recommendation.dto.RecommendationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Service for managing recommendations.
 * Orchestrates between different RecommendationProviders.
 */
@Service
public class RecommendationService {

    private final RecommendationProvider recommendationProvider;

    public RecommendationService(RecommendationProvider recommendationProvider) {
        this.recommendationProvider = recommendationProvider;
    }

    /**
     * Get personalized recommendations for a user.
     */
    public Page<RecommendationDto> getPersonalizedRecommendations(Long userId, Pageable pageable) {
        return recommendationProvider.getPersonalizedRecommendations(userId, pageable);
    }

    /**
     * Get popular events.
     */
    public Page<RecommendationDto> getPopularEvents(Pageable pageable) {
        return recommendationProvider.getPopularEvents(pageable);
    }

    /**
     * Get upcoming events.
     */
    public Page<RecommendationDto> getUpcomingEvents(Pageable pageable) {
        return recommendationProvider.getUpcomingEvents(pageable);
    }
}