package com.eventbooking.auth.controller;

import static org.hamcrest.Matchers.hasItems;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eventbooking.common.security.Role;
import com.eventbooking.support.AbstractIntegrationTest;
import com.eventbooking.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** End-to-end through the real security filter chain, Postgres and Redis. */
@AutoConfigureMockMvc
class AuthIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Passw0rd!";

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    UserService userService;
    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void registerReturns201WithTokenAndNeverLeaksPassword() throws Exception {
        String email = uniqueEmail();

        register(email, PASSWORD, "Jane Doe")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void passwordIsStoredAsBCryptHash() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD, "Jane Doe").andExpect(status().isCreated());

        String stored = jdbc.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class, email);

        assertThat(stored).startsWith("$2").isNotEqualTo(PASSWORD);
    }

    @Test
    void duplicateEmailIsRejectedCaseInsensitively() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD, "First").andExpect(status().isCreated());

        register(email.toUpperCase(), PASSWORD, "Second")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void invalidRegistrationListsEveryOffendingField() throws Exception {
        register("not-an-email", "short", " ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItems("email", "password", "fullName")));
    }

    @Test
    void loginWithValidCredentialsReturnsToken() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD, "Jane Doe").andExpect(status().isCreated());

        login(email, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD, "Jane Doe").andExpect(status().isCreated());

        login(email, "Wrong-pass1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void protectedEndpointWithoutTokenReturns401Json() throws Exception {
        mvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void protectedEndpointWithGarbageTokenReturns401() throws Exception {
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void meReturnsTheCallersProfile() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD, "Jane Doe").andExpect(status().isCreated());
        String token = tokenFor(email);

        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.fullName").value("Jane Doe"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void adminOnlyEndpointRejectsUsersAndAnonymousButAllowsAdmins() throws Exception {
        String userEmail = uniqueEmail();
        register(userEmail, PASSWORD, "Regular User").andExpect(status().isCreated());
        String userToken = tokenFor(userEmail);

        String adminEmail = uniqueEmail();
        userService.createUser(adminEmail, passwordEncoder.encode(PASSWORD), "Admin", Role.ADMIN);
        String adminToken = tokenFor(adminEmail);

        mvc.perform(get("/api/test/admin-only"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/test/admin-only").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mvc.perform(get("/api/test/admin-only").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ helpers

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.dev";
    }

    private ResultActions register(String email, String password, String fullName) throws Exception {
        return mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("email", email, "password", password, "fullName", fullName))));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))));
    }

    private String tokenFor(String email) throws Exception {
        String body = login(email, PASSWORD).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }
}
