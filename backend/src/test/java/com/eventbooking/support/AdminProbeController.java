package com.eventbooking.support;

import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Test-only endpoint used to prove role-based authorization works before real admin endpoints exist. */
@RestController
@RequestMapping("/api/test")
public class AdminProbeController {

    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> adminOnly() {
        return Map.of("status", "ok");
    }
}
