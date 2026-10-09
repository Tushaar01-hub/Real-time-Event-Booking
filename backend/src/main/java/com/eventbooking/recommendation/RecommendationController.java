package com.eventbooking.recommendation;

import com.eventbooking.recommendation.dto.RecommendationDto;
import com.eventbooking.common.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for event recommendations.
 */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /**
     * Get personalized recommendations for the current user.
     * Fallbacks to popular/upcoming if no user history is found.
     */
    @GetMapping
    public ResponseEntity<Page<RecommendationDto>> getRecommendations(
            @AuthenticationPrincipal AuthenticatedUser  user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.unsorted());

        if (user == null) {
            // Anonymous user gets upcoming events
            return ResponseEntity.ok(recommendationService.getUpcomingEvents(pageable));
        }

        return ResponseEntity.ok(recommendationService.getPersonalizedRecommendations(user.id(), pageable));
    }

    /**
     * Get popular events (explicit endpoint).
     */
    @GetMapping("/popular")
    public ResponseEntity<Page<RecommendationDto>> getPopularEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.unsorted());
        return ResponseEntity.ok(recommendationService.getPopularEvents(pageable));
    }

    /**
     * Get upcoming events (explicit endpoint).
     */
    @GetMapping("/upcoming")
    public ResponseEntity<Page<RecommendationDto>> getUpcomingEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.unsorted());
        return ResponseEntity.ok(recommendationService.getUpcomingEvents(pageable));
    }
}