package com.eventbooking.recommendation;

import com.eventbooking.catalog.category.Category;
import com.eventbooking.catalog.event.Event;
import com.eventbooking.catalog.show.Show;
import com.eventbooking.auth.security.JwtService;
import com.eventbooking.booking.entity.Booking;
import com.eventbooking.booking.repository.BookingRepository;
import com.eventbooking.catalog.category.Category;
import com.eventbooking.catalog.category.CategoryRepository;
import com.eventbooking.catalog.event.Event;
import com.eventbooking.catalog.event.EventRepository;
import com.eventbooking.catalog.show.Show;
import com.eventbooking.catalog.show.ShowRepository;
import com.eventbooking.common.security.Role;
import com.eventbooking.support.AbstractIntegrationTest;
import com.eventbooking.user.entity.User;
import com.eventbooking.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class RecommendationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User testUser;
    private Category testCategory;
    private Event testEvent;
    private Show testShow;

    @BeforeEach
    void setUp() {
        // Clear existing test data
        bookingRepository.deleteAll();
        showRepository.deleteAll();
        eventRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        // Create test data
        testUser = createTestUser("user@example.com", "password");
        testCategory = createTestCategory("Music", "music");
        testEvent = createTestEvent("Concert", "Great music", testCategory, "New York");
        testShow = createTestShow(testEvent, Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200), "Venue 1");

        // Create a confirmed booking for the user
        createTestBooking(testUser, testShow, BigDecimal.valueOf(50.00), "CONFIRMED");
    }

    private User createTestUser(String email, String password) {
        User user = new User(email, passwordEncoder.encode(password), "Test User", Role.USER);
        return userRepository.saveAndFlush(user);
    }

    private Category createTestCategory(String name, String slug) {
        Category category = new Category(name, slug);
        return categoryRepository.saveAndFlush(category);
    }

    private Event createTestEvent(String title, String description, Category category, String location) {
        Event event = new Event(title, description, category, location, null);
        return eventRepository.saveAndFlush(event);
    }

    private Show createTestShow(Event event, Instant startTime, Instant endTime, String venue) {
        Show show = new Show(event, startTime, endTime, venue);
        return showRepository.saveAndFlush(show);
    }

    private Booking createTestBooking(User user, Show show, BigDecimal totalAmount, String status) {
        Booking booking = new Booking(user, show, totalAmount, Instant.now().plusSeconds(3600));
        booking.setStatus(status);
        return bookingRepository.saveAndFlush(booking);
    }

    private String createToken(User user) {
        return jwtService.issue(user.getId(), user.getEmail(), user.getRole()).value();
    }

    @Test
    void getPersonalizedRecommendations_WithHistory_Success() throws Exception {
        // Create another event in the same category
        Event similarEvent = createTestEvent("Another Concert", "More great music", testCategory, "New York");
        Show similarShow = createTestShow(similarEvent, Instant.now().plusSeconds(7200), Instant.now().plusSeconds(10800), "Venue 2");

        mockMvc.perform(get("/api/recommendations")
                .header("Authorization", "Bearer " + createToken(testUser))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].categoryName").value("Music"))
                .andExpect(jsonPath("$.content[0].reason").value("Based on your interest in Music"));
    }

    @Test
    void getPersonalizedRecommendations_NoHistory_FallbackToPopular() throws Exception {
        // Create a user with no bookings
        User newUser = createTestUser("newuser@example.com", "password");

        mockMvc.perform(get("/api/recommendations")
                .header("Authorization", "Bearer " + createToken(newUser))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reason").value("Popular event"));
    }

    @Test
    void getPopularEvents_Success() throws Exception {
        mockMvc.perform(get("/api/recommendations/popular")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reason").value("Popular event"));
    }

    @Test
    void getUpcomingEvents_Success() throws Exception {
        mockMvc.perform(get("/api/recommendations/upcoming")
                .header("Authorization", "Bearer " + createToken(testUser))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reason").value("Upcoming event"));
    }

    @Test
    void getRecommendations_AnonymousUser_GetsUpcoming() throws Exception {
        mockMvc.perform(get("/api/recommendations")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reason").value("Upcoming event"));
    }
}