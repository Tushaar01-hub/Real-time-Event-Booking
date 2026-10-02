package com.eventbooking.user.bootstrap;

import com.eventbooking.common.security.Role;
import com.eventbooking.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first ADMIN account from environment variables (ADMIN_EMAIL / ADMIN_PASSWORD).
 * Public registration can only ever create USER accounts, and no credentials live in the code.
 * Does nothing if the variables are unset or the account already exists.
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminBootstrapRunner(UserService userService,
                                PasswordEncoder passwordEncoder,
                                @Value("${app.admin.email:}") String adminEmail,
                                @Value("${app.admin.password:}") String adminPassword) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.info("ADMIN_EMAIL/ADMIN_PASSWORD not set: skipping admin bootstrap");
            return;
        }
        if (adminPassword.length() < 8) {
            log.warn("ADMIN_PASSWORD is shorter than 8 characters: skipping admin bootstrap");
            return;
        }
        if (userService.existsByEmail(adminEmail)) {
            return;
        }
        userService.createUser(adminEmail, passwordEncoder.encode(adminPassword), "Administrator", Role.ADMIN);
        log.info("Created bootstrap admin account {}", adminEmail);
    }
}
