package com.eventbooking.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.eventbooking.auth.dto.AuthResponse;
import com.eventbooking.auth.dto.LoginRequest;
import com.eventbooking.auth.dto.RegisterRequest;
import com.eventbooking.auth.security.IssuedToken;
import com.eventbooking.auth.security.JwtService;
import com.eventbooking.common.exception.EmailAlreadyExistsException;
import com.eventbooking.common.exception.InvalidCredentialsException;
import com.eventbooking.common.security.Role;
import com.eventbooking.user.entity.User;
import com.eventbooking.user.mapper.UserMapper;
import com.eventbooking.user.service.UserService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String PASSWORD = "Passw0rd!";

    @Mock
    UserService userService;
    @Mock
    JwtService jwtService;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4); // low cost: fast tests
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userService, encoder, jwtService, new UserMapper());
    }

    @Test
    void registerHashesPasswordAndAlwaysAssignsUserRole() {
        when(userService.createUser(anyString(), anyString(), anyString(), any(Role.class)))
                .thenAnswer(inv -> new User(inv.getArgument(0), inv.getArgument(1),
                        inv.getArgument(2), inv.getArgument(3)));
        when(jwtService.issue(any(), anyString(), any(Role.class))).thenReturn(new IssuedToken("tok", 3600));

        AuthResponse response = authService.register(new RegisterRequest("new@example.com", PASSWORD, " Jane "));

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(userService).createUser(eq("new@example.com"), hash.capture(), eq("Jane"), eq(Role.USER));
        assertThat(hash.getValue()).isNotEqualTo(PASSWORD);
        assertThat(encoder.matches(PASSWORD, hash.getValue())).isTrue();
        assertThat(response.token()).isEqualTo("tok");
        assertThat(response.user().role()).isEqualTo(Role.USER);
    }

    @Test
    void registerPropagatesDuplicateEmail() {
        when(userService.createUser(anyString(), anyString(), anyString(), any(Role.class)))
                .thenThrow(new EmailAlreadyExistsException());

        assertThatThrownBy(() -> authService.register(new RegisterRequest("dup@example.com", PASSWORD, "Jane")))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void loginSucceedsWithCorrectPassword() {
        User user = new User("a@b.com", encoder.encode(PASSWORD), "A", Role.USER);
        when(userService.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(jwtService.issue(any(), eq("a@b.com"), eq(Role.USER))).thenReturn(new IssuedToken("tok", 3600));

        AuthResponse response = authService.login(new LoginRequest("a@b.com", PASSWORD));

        assertThat(response.token()).isEqualTo("tok");
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void loginRejectsWrongPasswordWithoutIssuingToken() {
        User user = new User("a@b.com", encoder.encode(PASSWORD), "A", Role.USER);
        when(userService.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("a@b.com", "Wrong-pass1")))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(jwtService);
    }

    @Test
    void unknownEmailAndWrongPasswordGiveTheSameError() {
        User user = new User("a@b.com", encoder.encode(PASSWORD), "A", Role.USER);
        when(userService.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(userService.findByEmail("ghost@b.com")).thenReturn(Optional.empty());

        InvalidCredentialsException wrongPassword = catchInvalid(new LoginRequest("a@b.com", "Wrong-pass1"));
        InvalidCredentialsException unknownEmail = catchInvalid(new LoginRequest("ghost@b.com", PASSWORD));

        assertThat(unknownEmail.getCode()).isEqualTo(wrongPassword.getCode());
        assertThat(unknownEmail.getMessage()).isEqualTo(wrongPassword.getMessage());
    }

    private InvalidCredentialsException catchInvalid(LoginRequest request) {
        try {
            authService.login(request);
        } catch (InvalidCredentialsException e) {
            return e;
        }
        throw new AssertionError("Expected InvalidCredentialsException");
    }
}
