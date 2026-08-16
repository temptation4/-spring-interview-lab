package com.interview.labs.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Split out from SecurityConfig on purpose: SecurityConfig takes a JwtAuthenticationFilter
 * constructor dependency, which resolves down to DemoUserDetailsService -> PasswordEncoder.
 * If PasswordEncoder were a @Bean method on SecurityConfig itself, creating that bean would
 * require SecurityConfig to already exist — a circular reference Spring refuses to resolve.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
