package com.eventbooking.auth.service;

import com.eventbooking.auth.dto.AuthResponse;
import com.eventbooking.auth.dto.LoginRequest;
import com.eventbooking.auth.dto.RegisterRequest;
import com.eventbooking.auth.security.IssuedToken;
import com.eventbooking.auth.security.JwtService;
import com.eventbooking.common.exception.InvalidCredentialsException;
import com.eventbooking.common.security.Role;
import com.eventbooking.user.entity.User;
import com.eventbooking.user.mapper.UserMapper;
import com.eventbooking.user.service.UserService;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    /** Verified against when the email is unknown, so both failure paths cost one BCrypt check. */
    private final String dummyHash;

    public AuthService(UserService userService, PasswordEncoder passwordEncoder,
                       JwtService jwtService, UserMapper userMapper) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.dummyHash = passwordEncoder.encode("timing-equalisation-placeholder");
    }

    /** Public registration can only ever create a USER; admins come from the bootstrap runner. */
    public AuthResponse register(RegisterRequest request) {
        User user = userService.createUser(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                Role.USER);
        return toAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        Optional<User> user = userService.findByEmail(request.email());
        String hash = user.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
        if (user.isEmpty() || !passwordMatches) {
            throw new InvalidCredentialsException();
        }
        return toAuthResponse(user.get());
    }

    private AuthResponse toAuthResponse(User user) {
        IssuedToken token = jwtService.issue(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(token.value(), "Bearer", token.expiresInSeconds(), userMapper.toResponse(user));
    }
}
