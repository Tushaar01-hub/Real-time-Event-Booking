package com.eventbooking.user.dto;

import com.eventbooking.common.security.Role;
import java.time.Instant;

public record UserResponse(Long id, String email, String fullName, Role role, Instant createdAt) {
}
