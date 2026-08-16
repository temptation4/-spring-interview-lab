# Spring Security Interview Notes

> Complete Revision Guide for Java & Spring Boot Interviews

> Every concept below is backed by real code in `com.interview.labs.security` in this same lab — run the app, hit `/auth/login`, then `/secure/user` or `/secure/admin` with the token, and watch it actually enforce the rules.

## Table of Contents

1.  What Problem Security Solves
2.  The Filter Chain
3.  Authentication vs Authorization
4.  `UserDetailsService` & `UserDetails`
5.  `PasswordEncoder`
6.  `AuthenticationManager` & `AuthenticationProvider`
7.  JWT — Structure & Why Stateless
8.  `JwtAuthenticationFilter`, End to End
9.  `SecurityContextHolder`
10. Method Security — `@PreAuthorize`
11. 401 vs 403
12. Why This Lab Only Locks Down `/secure/**`
13. Cheat Sheet
14. Important Interview Questions

------------------------------------------------------------------------

## 1. What Problem Security Solves

Two separate questions, every framework answers them separately:

-   **Authentication** — who are you? (login)
-   **Authorization** — what are you allowed to do? (permissions)

Spring Security answers both, but as two distinct phases in the request pipeline — that separation is the single most important thing to understand before any of the annotations make sense.

------------------------------------------------------------------------

## 2. The Filter Chain

Spring Security is a **chain of servlet filters** sitting in front of `DispatcherServlet`. Every request passes through it before your controller ever sees it.

    Client
      ↓
    Security Filter Chain (ordered filters)
      ↓
    JwtAuthenticationFilter          ← custom, this lab
      ↓
    SecurityContextHolder populated (or left empty)
      ↓
    DispatcherServlet → Controller
      ↓
    authorizeHttpRequests / @PreAuthorize decision
      ↓
    200 OK  or  401 / 403

This is why a custom filter (`JwtAuthenticationFilter`) can sit *before* `UsernamePasswordAuthenticationFilter` and short-circuit the whole "is this user authenticated" question before Spring's built-in machinery even runs — exactly what this lab does.

------------------------------------------------------------------------

## 3. Authentication vs Authorization

| | Authentication | Authorization |
|---|---|---|
| Question | Who are you? | What can you do? |
| In this lab | `POST /auth/login` verifies username+password | `@PreAuthorize("hasRole('ADMIN')")` on `/secure/admin` |
| Failure status | 401 Unauthorized | 403 Forbidden |
| Spring abstraction | `AuthenticationManager` | `AccessDecisionManager` / `@PreAuthorize` |

------------------------------------------------------------------------

## 4. `UserDetailsService` & `UserDetails`

`UserDetailsService` has one job: given a username, return a `UserDetails` (password hash + authorities). Spring Security never touches your user table directly — it always goes through this interface, which is why swapping storage (in-memory → JPA → LDAP) never touches the filter chain.

## Code — `DemoUserDetailsService.java`

``` java
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
    public UserDetails loadUserByUsername(String username) {
        UserDetails user = users.get(username);
        if (user == null) throw new UsernameNotFoundException("No such user: " + username);
        return user;
    }
}
```

This lab uses an in-memory map on purpose — the point is the auth *mechanics*, not the storage layer. Section 3 already covers JPA-backed persistence in depth; swapping this for a `UserRepository.findByUsername(...)` call is a one-file change.

------------------------------------------------------------------------

## 5. `PasswordEncoder`

Passwords are **never** stored or compared in plaintext. `BCryptPasswordEncoder` hashes with a random salt baked into the output, so the same password produces a different hash every time — `matches()` re-derives and compares, `equals()` on the hash strings would always be wrong.

``` java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

**Design note, worth remembering as an interview answer:** this bean lives in its own `PasswordEncoderConfig` class, *not* on `SecurityConfig`. `SecurityConfig` takes `JwtAuthenticationFilter` as a constructor dependency → which needs `DemoUserDetailsService` → which needs `PasswordEncoder`. If `PasswordEncoder` were a `@Bean` method *on* `SecurityConfig`, creating that bean would require `SecurityConfig` to already be fully constructed — a genuine circular reference Spring Boot 3.2 refuses to resolve at startup. Splitting it into a separate `@Configuration` class breaks the cycle. (This is a real bug that was hit and fixed while building this section — not a hypothetical.)

------------------------------------------------------------------------

## 6. `AuthenticationManager` & `AuthenticationProvider`

`AuthenticationManager` is the entry point `AuthController` calls; it delegates to one or more `AuthenticationProvider`s. This lab wires a `DaoAuthenticationProvider`, which is the standard "look the user up via `UserDetailsService`, compare the password via `PasswordEncoder`" implementation.

``` java
@Bean
public AuthenticationProvider authenticationProvider(
        UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {

    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return provider;
}
```

**Endpoint:** `POST /auth/login`

``` java
@PostMapping("/login")
public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    try {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return LoginResponse.bearer(jwtService.generateToken(userDetails));

    } catch (BadCredentialsException invalidLogin) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }
}
```

------------------------------------------------------------------------

## 7. JWT — Structure & Why Stateless

A JWT is three base64url segments: `header.payload.signature`. The server signs the payload with a secret key; anyone can *read* the payload (it's just base64, not encryption), but only the server can *produce a valid signature* for it — that's what makes it trustworthy without a database lookup.

    header    { "alg": "HS256" }
    payload   { "sub": "user", "roles": [...], "iat": ..., "exp": ... }
    signature HMAC-SHA256(header + "." + payload, secret)

**Why stateless matters:** there's no server-side session to store or look up — the token itself carries everything needed to authenticate the next request. That's what `SessionCreationPolicy.STATELESS` in `SecurityConfig` declares explicitly.

## Code — `JwtService.java`

``` java
public String generateToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("roles", userDetails.getAuthorities());

    return Jwts.builder()
            .setClaims(claims)
            .setSubject(userDetails.getUsername())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
}
```

The signing key is loaded once from `jwt.secret` in `application.properties` and reused for the app's whole lifetime — **not** regenerated on every restart. (Generating a fresh random key per boot is a classic bug: every previously issued token instantly becomes invalid, and tokens issued by one instance won't validate on another in a multi-instance deployment.)

------------------------------------------------------------------------

## 8. `JwtAuthenticationFilter`, End to End

``` java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {

    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        chain.doFilter(request, response);
        return;
    }

    try {
        String token = authHeader.substring(7);
        String username = jwtService.extractUsername(token);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtService.isTokenValid(token, userDetails)) {
                var authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
    } catch (JwtException | IllegalArgumentException malformedOrExpiredToken) {
        // leave the context unauthenticated — the request falls through to a 401/403, not a 500
    }

    chain.doFilter(request, response);
}
```

Two details worth calling out because they're easy to get wrong:

-   **No token at all is not an error** — it's a normal request that continues down the chain unauthenticated. Only *malformed or expired* tokens are caught explicitly, so a bad token turns into a clean 401 instead of an unhandled `JwtException` bubbling up as a 500.
-   The filter itself never rejects anything — it only *populates or doesn't populate* the security context. The actual accept/reject decision happens later, in `authorizeHttpRequests` and `@PreAuthorize`.

**End-to-end flow:**

    POST /auth/login {username, password}
      → AuthenticationManager → DaoAuthenticationProvider
      → DemoUserDetailsService.loadUserByUsername()
      → PasswordEncoder.matches()
      → JwtService.generateToken() → signed JWT returned

    GET /secure/user  Authorization: Bearer <token>
      → JwtAuthenticationFilter validates signature + expiry
      → loads UserDetails, sets SecurityContextHolder
      → authorizeHttpRequests: "/secure/**" requires authenticated() → passes
      → controller runs

------------------------------------------------------------------------

## 9. `SecurityContextHolder`

A `ThreadLocal`-backed holder for the current request's `Authentication`. Once `JwtAuthenticationFilter` sets it, everything downstream — `authorizeHttpRequests`, `@PreAuthorize`, `Authentication authentication` as a controller parameter — reads from the same place, regardless of *how* the principal got authenticated (JWT here; could be Basic, OAuth2, anything).

``` java
@GetMapping("/user")
public String userOnly(Authentication authentication) {
    return "Hello " + authentication.getName() +
           " — you're authenticated (roles: " + authentication.getAuthorities() + ")";
}
```

------------------------------------------------------------------------

## 10. Method Security — `@PreAuthorize`

`authorizeHttpRequests` matches on the **URL**. `@PreAuthorize` matches on the **method**, and runs independently — a request can pass the URL-level check and still be rejected at the method level.

``` java
@EnableMethodSecurity   // on SecurityConfig — required for @PreAuthorize to be evaluated at all
```

``` java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin")
public String adminOnly(Authentication authentication) {
    return "Hello admin " + authentication.getName() + " — ROLE_ADMIN confirmed";
}
```

`/secure/admin` is only `.authenticated()` at the URL level in `SecurityConfig` — **any** logged-in user gets past that check. It's `@PreAuthorize("hasRole('ADMIN')")` on the method itself that actually enforces the role. Log in as `user`/`password` and hit `/secure/admin` to see the 403 happen at the method layer, not the URL layer.

------------------------------------------------------------------------

## 11. 401 vs 403

The single most commonly confused pair in this whole topic:

| Status | Means | When it fires here |
|---|---|---|
| **401 Unauthorized** | *You haven't proven who you are* | No `Authorization` header, or a malformed/expired token, on a `/secure/**` request |
| **403 Forbidden** | *We know who you are — you're just not allowed* | A valid token for `user`, hitting `/secure/admin` (`@PreAuthorize` rejects it) |

Getting this right requires an explicit `AuthenticationEntryPoint` — without one, Spring Security's default behavior returns 403 for *both* cases, which is technically wrong and a common source of confused API consumers:

``` java
.exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedEntryPoint()))

private AuthenticationEntryPoint unauthorizedEntryPoint() {
    return (request, response, authException) ->
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid token");
}
```

------------------------------------------------------------------------

## 12. Why This Lab Only Locks Down `/secure/**`

Sections 1-4 (`core`, `mvc`, `jpa`, `aop`) stay completely open — you can `curl` them with no login step, exactly as before this section existed. `SecurityConfig`'s rule is deliberately narrow:

``` java
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/auth/**").permitAll()
        .requestMatchers("/secure/**").authenticated()
        .anyRequest().permitAll()
)
```

This keeps the auth mechanics isolated in one sandbox (`/auth/**` + `/secure/**`) instead of retrofitting login onto every endpoint in the lab, which would make the security-specific behavior harder to isolate from everything else this repo teaches.

------------------------------------------------------------------------

## 13. Cheat Sheet

    Client
      ↓
    POST /auth/login → AuthenticationManager → DaoAuthenticationProvider
      ↓
    UserDetailsService.loadUserByUsername() + PasswordEncoder.matches()
      ↓
    JwtService.generateToken() → signed JWT returned to client
      ↓
    every later request: Authorization: Bearer <token>
      ↓
    JwtAuthenticationFilter validates signature + expiry
      ↓
    SecurityContextHolder populated
      ↓
    authorizeHttpRequests (URL rule)  →  @PreAuthorize (method rule)
      ↓
    200 OK  /  401 (not authenticated)  /  403 (authenticated, wrong role)

------------------------------------------------------------------------

## 14. Important Interview Questions

-   What's the difference between authentication and authorization?
-   Where does `JwtAuthenticationFilter` sit relative to `UsernamePasswordAuthenticationFilter`, and why does that order matter?
-   Why can't a JWT be revoked before it expires? What are the usual workarounds (short expiry + refresh token, server-side blocklist)?
-   What happens if the JWT signing secret is regenerated on every app restart?
-   Why does `PasswordEncoder` live in its own `@Configuration` class instead of on `SecurityConfig` in this lab?
-   What's the difference between `authorizeHttpRequests` and `@PreAuthorize`, and can a request pass one and fail the other?
-   Why does a request with no `Authorization` header return 401, but a valid token for the wrong role return 403?
-   Why is `SessionCreationPolicy.STATELESS` set here, and what would change if it weren't?

------------------------------------------------------------------------

## Where This Goes Next

Section 6 — [Spring Cloud](07-Spring-Cloud-README.md) — covers what changes once this app isn't the only service: service discovery so callers don't hardcode a host:port, externalized config so a secret like `jwt.secret` can be rotated without a redeploy, and an API Gateway as the single point where cross-cutting concerns like auth get centralized instead of duplicated per service.
