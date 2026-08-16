package com.interview.labs.security;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The sandbox {@link SecurityConfig} actually locks down. Hit these with:
 * {@code Authorization: Bearer <token from POST /auth/login>}
 */
@RestController
@RequestMapping("/secure")
public class SecuredController {

    // any authenticated principal — enforced by SecurityConfig's authorizeHttpRequests rule
    @GetMapping("/user")
    public String userOnly(Authentication authentication) {
        return "Hello " + authentication.getName() + " — you're authenticated (roles: "
                + authentication.getAuthorities() + ")";
    }

    // method-level authorization — enforced by @EnableMethodSecurity, independent of the URL rules above
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public String adminOnly(Authentication authentication) {
        return "Hello admin " + authentication.getName() + " — ROLE_ADMIN confirmed";
    }
}
