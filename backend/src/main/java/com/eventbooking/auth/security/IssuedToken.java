package com.eventbooking.auth.security;

public record IssuedToken(String value, long expiresInSeconds) {
}
