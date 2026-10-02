package com.eventbooking.common.security;

/**
 * The principal placed in the SecurityContext for a valid JWT. Built purely from
 * token claims, so authenticating a request needs no database lookup.
 * Inject it in controllers with {@code @AuthenticationPrincipal}.
 */
public record AuthenticatedUser(Long id, String email, Role role) {
}
