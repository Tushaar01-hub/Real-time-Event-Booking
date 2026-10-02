package com.eventbooking.auth.dto;

import com.eventbooking.user.dto.UserResponse;

public record AuthResponse(String token, String tokenType, long expiresInSeconds, UserResponse user) {
}
