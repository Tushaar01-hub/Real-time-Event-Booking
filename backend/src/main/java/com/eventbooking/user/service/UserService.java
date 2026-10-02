package com.eventbooking.user.service;

import com.eventbooking.common.exception.EmailAlreadyExistsException;
import com.eventbooking.common.exception.ResourceNotFoundException;
import com.eventbooking.common.security.Role;
import com.eventbooking.user.dto.UserResponse;
import com.eventbooking.user.entity.User;
import com.eventbooking.user.mapper.UserMapper;
import com.eventbooking.user.repository.UserRepository;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public API of the user module. Other modules (e.g. auth) go through this service
 * instead of touching the repository directly.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    /**
     * Creates a user with an already-hashed password. Emails are stored lower-case.
     * The existence check gives a friendly error; the unique index is what actually
     * guarantees uniqueness when two registrations race.
     */
    @Transactional
    public User createUser(String email, String passwordHash, String fullName, Role role) {
        String normalizedEmail = normalize(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException();
        }
        try {
            return userRepository.saveAndFlush(new User(normalizedEmail, passwordHash, fullName, role));
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyExistsException();
        }
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(normalize(email));
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(normalize(email));
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return userRepository.findById(userId)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
