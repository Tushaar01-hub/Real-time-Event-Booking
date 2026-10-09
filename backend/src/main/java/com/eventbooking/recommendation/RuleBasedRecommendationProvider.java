package com.eventbooking.recommendation;

import com.eventbooking.booking.repository.BookingRepository;
import com.eventbooking.catalog.event.EventRepository;
import com.eventbooking.catalog.show.Show;
import com.eventbooking.catalog.show.ShowRepository;
import com.eventbooking.recommendation.dto.RecommendationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Rule-based implementation of RecommendationProvider.
 */
@Service
@Transactional(readOnly = true)
public class RuleBasedRecommendationProvider implements RecommendationProvider {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final ShowRepository showRepository;

    public RuleBasedRecommendationProvider(BookingRepository bookingRepository,
                                      EventRepository eventRepository,
                                      ShowRepository showRepository) {
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
        this.showRepository = showRepository;
    }

    @Override
    public Page<RecommendationDto> getPersonalizedRecommendations(Long userId, Pageable pageable) {
        // 1. Get user's booked categories and locations
        List<Long> categoryIds = bookingRepository.findBookedCategoryIdsByUserId(userId);
        List<String> locations = bookingRepository.findBookedLocationsByUserId(userId);
        System.out.println("categoryIds = " + categoryIds);
        System.out.println("locations = " + locations);
        // 2. If no history, fall back to popular/upcoming
        if (categoryIds.isEmpty() && locations.isEmpty()) {
            return getPopularEvents(pageable);
        }

        // 3. Find events matching user's preferences
        Page<com.eventbooking.catalog.event.Event> eventPage = eventRepository.findByCategoryIdsOrLocations(categoryIds, locations, pageable);
        if (eventPage.isEmpty()) {
            return getPopularEvents(pageable);
        }
        List<com.eventbooking.catalog.event.Event> events = eventPage.getContent();
        List<Long> eventIds = events.stream().map(com.eventbooking.catalog.event.Event::getId).toList();

        // 4. Get upcoming shows for these events
        if (eventIds.isEmpty()) {
            return getPopularEvents(pageable);
        }
        List<Show> upcomingShows = showRepository.findUpcomingShowsByEventIds(eventIds, Instant.now());

        // 5. Convert to DTOs with reasons
        List<RecommendationDto> recommendations = new ArrayList<>();
        for (Show show : upcomingShows) {
            BigDecimal minPrice = showRepository.findMinPriceForEvent(show.getEvent().getId(), Instant.now());
            String reason = "Based on your interest in " +
                    (categoryIds.contains(show.getEvent().getCategory().getId()) ? show.getEvent().getCategory().getName() : show.getEvent().getLocation());

            recommendations.add(new RecommendationDto(
                    show.getEvent().getId(),
                    show.getEvent().getTitle(),
                    show.getEvent().getCategory().getName(),
                    show.getEvent().getLocation(),
                    show.getStartTime(),
                    minPrice,
                    show.getEvent().getPosterUrl(),
                    reason,
                    1.0 // Default score for rule-based
            ));
        }

        return new PageImpl<>(recommendations, pageable, recommendations.size());
    }

    @Override
    public Page<RecommendationDto> getPopularEvents(Pageable pageable) {
        // Get most booked events
        List<Long> popularEventIds = showRepository.findPopularEventIds(pageable).getContent();

        // Get upcoming shows for these events
        List<Show> upcomingShows = showRepository.findUpcomingShowsByEventIds(popularEventIds, Instant.now());

        // Convert to DTOs
        List<RecommendationDto> recommendations = new ArrayList<>();
        for (Show show : upcomingShows) {
            BigDecimal minPrice = showRepository.findMinPriceForEvent(show.getEvent().getId(), Instant.now());

            recommendations.add(new RecommendationDto(
                    show.getEvent().getId(),
                    show.getEvent().getTitle(),
                    show.getEvent().getCategory().getName(),
                    show.getEvent().getLocation(),
                    show.getStartTime(),
                    minPrice,
                    show.getEvent().getPosterUrl(),
                    "Popular event",
                    1.0
            ));
        }

        return new PageImpl<>(recommendations, pageable, recommendations.size());
    }

    @Override
    public Page<RecommendationDto> getRelatedEvents(Long eventId, Pageable pageable) {
        // Get events in the same category
        Page<com.eventbooking.catalog.event.Event> relatedEventPage = eventRepository.findByCategoryId(eventRepository.findById(eventId).get().getCategory().getId(), pageable);
        if (relatedEventPage.isEmpty()) {
            return getPopularEvents(pageable);
        }
        List<com.eventbooking.catalog.event.Event> relatedEvents = relatedEventPage.getContent();
        List<Long> relatedEventIds = relatedEvents.stream().map(com.eventbooking.catalog.event.Event::getId).toList();

        // Get upcoming shows for these events
        List<Show> upcomingShows = showRepository.findUpcomingShowsByEventIds(relatedEventIds, Instant.now());
        if (upcomingShows.isEmpty()) {
            return getPopularEvents(pageable);
        }

        // Convert to DTOs
        List<RecommendationDto> recommendations = new ArrayList<>();
        for (Show show : upcomingShows) {
            BigDecimal minPrice = showRepository.findMinPriceForEvent(show.getEvent().getId(), Instant.now());

            recommendations.add(new RecommendationDto(
                    show.getEvent().getId(),
                    show.getEvent().getTitle(),
                    show.getEvent().getCategory().getName(),
                    show.getEvent().getLocation(),
                    show.getStartTime(),
                    minPrice,
                    show.getEvent().getPosterUrl(),
                    "Similar to what you liked",
                    1.0
            ));
        }

        return new PageImpl<>(recommendations, pageable, recommendations.size());
    }

    @Override
    public Page<RecommendationDto> getUpcomingEvents(Pageable pageable) {
        // Get upcoming events
        List<Long> upcomingEventIds = showRepository.findUpcomingEventIds(Instant.now(), pageable).getContent();

        // Get upcoming shows for these events
        List<Show> upcomingShows = showRepository.findUpcomingShowsByEventIds(upcomingEventIds, Instant.now());

        // Convert to DTOs
        List<RecommendationDto> recommendations = new ArrayList<>();
        for (Show show : upcomingShows) {
            BigDecimal minPrice = showRepository.findMinPriceForEvent(show.getEvent().getId(), Instant.now());

            recommendations.add(new RecommendationDto(
                    show.getEvent().getId(),
                    show.getEvent().getTitle(),
                    show.getEvent().getCategory().getName(),
                    show.getEvent().getLocation(),
                    show.getStartTime(),
                    minPrice,
                    show.getEvent().getPosterUrl(),
                    "Upcoming event",
                    1.0
            ));
        }

        return new PageImpl<>(recommendations, pageable, recommendations.size());
    }
}