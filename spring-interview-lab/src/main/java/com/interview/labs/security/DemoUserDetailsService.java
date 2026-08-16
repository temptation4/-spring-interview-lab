package com.interview.labs.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * In-memory user store so the JWT demo runs with zero extra setup (no DB migration needed
 * just to log in). Section 3 already covers JPA-backed persistence — swapping this for a
 * {@code UserRepository} lookup is a one-file change if you want to combine the two.
 */
@Service
public class DemoUserDetailsService implements UserDetailsService {

    private final Map<String, UserDetails> users;

    public DemoUserDetailsService(PasswordEncoder passwordEncoder) {
        this.users = Map.of(
                "user", User.withUsername("user")
                        .password(passwordEncoder.encode("password"))
                        .roles("USER")
                        .build(),
                "admin", User.withUsername("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .roles("USER", "ADMIN")
                        .build()
        );
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails user = users.get(username);
        if (user == null) {
            throw new UsernameNotFoundException("No such user: " + username);
        }
        return user;
    }
}
