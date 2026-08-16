# Spring Cloud Interview Notes

> Complete Revision Guide for Java & Spring Boot Interviews

> The client-side code below is real and in this lab (`com.interview.labs.cloud`) — the app boots and serves every other section fine with **zero** extra infrastructure running. The concepts that need an actual Eureka/Config Server running to fully observe are called out explicitly, the same way Section 3 (JPA) already requires a running MySQL to see anything beyond a stack trace.

## Table of Contents

1.  The Problem: One App Becomes Many
2.  Service Discovery — Eureka
3.  `DiscoveryClient` & `@LoadBalanced`
4.  Externalized Configuration — Config Server
5.  `@RefreshScope`
6.  API Gateway
7.  Why This Lab Doesn't Ship a Gateway
8.  Running the Full Picture Locally
9.  Cheat Sheet
10. Important Interview Questions

------------------------------------------------------------------------

## 1. The Problem: One App Becomes Many

Sections 1-5 are all one Spring Boot process. Spring Cloud is what changes once there's more than one:

-   Service A needs to call Service B — but B's host:port changes every deploy. Hardcoding it doesn't scale.
-   Every service needs the same DB credentials / feature flags — copy-pasting `application.properties` N times doesn't scale, and rotating a secret means N redeploys.
-   Clients need one URL to talk to, not N — and auth, rate limiting, and logging shouldn't be reimplemented in every service.

Three problems, three pieces of Spring Cloud:

    Problem                          Piece
    ────────────────────────────     ──────────────────
    "Where is service B right now?"  Service Discovery (Eureka)
    "Same config, many services"     Config Server
    "One entry point for clients"    API Gateway

------------------------------------------------------------------------

## 2. Service Discovery — Eureka

Instead of a client hardcoding `http://192.168.1.12:8081`, every service **registers itself** with a Eureka server on startup under a logical name (`spring.application.name`), and heartbeats periodically to prove it's still alive. Callers ask Eureka "who is `spring-interview-lab` right now?" instead of hardcoding an address.

    Service A                     Eureka Server                  Service B
        │                              │                              │
        │──── register (on boot) ─────▶│◀──── register (on boot) ─────│
        │──── heartbeat (30s) ────────▶│◀──── heartbeat (30s) ────────│
        │                              │                              │
        │──── "where is B?" ──────────▶│                              │
        │◀─── [instance list] ─────────│                              │

## Code — this lab as a Eureka *client*

`application.properties`:

``` properties
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
```

Adding `spring-cloud-starter-netflix-eureka-client` to the classpath is enough — Spring Boot auto-registers on startup, no annotation required in current Spring Cloud versions. Run this app with **no Eureka server up** and you'll see retry warnings in the console — that's expected and non-fatal, the app still starts and serves every other endpoint normally. Run an actual Eureka server on `:8761` and this app shows up in its dashboard within a few seconds.

------------------------------------------------------------------------

## 3. `DiscoveryClient` & `@LoadBalanced`

`DiscoveryClient` is the programmatic way to ask "what's registered right now" — the same registry Eureka's dashboard reads from.

## Code — `DiscoveryController.java`

``` java
@GetMapping("/services")
public List<String> services() {
    return discoveryClient.getServices();
}

@GetMapping("/instances/{serviceName}")
public List<ServiceInstance> instances(@PathVariable String serviceName) {
    return discoveryClient.getInstances(serviceName);
}
```

**Endpoint:** `GET /cloud/services` — returns `[]` with no Eureka server running (not an error), or a real list of registered service names once one is up.

`@LoadBalanced` is the piece that turns a logical service name into an actual call. A plain `RestTemplate` needs a real host:port. A `@LoadBalanced` one resolves the hostname against the service registry first:

``` java
@Bean
@LoadBalanced
public RestTemplate loadBalancedRestTemplate() {
    return new RestTemplate();
}
```

``` java
@GetMapping("/call/{serviceName}/{path}")
public ResponseEntity<String> callByServiceName(@PathVariable String serviceName, @PathVariable String path) {
    String url = "http://" + serviceName + "/" + path;   // "serviceName" here is NOT a real hostname
    return loadBalancedRestTemplate.getForEntity(url, String.class);
}
```

`http://spring-interview-lab/users` is not a resolvable DNS name — `@LoadBalanced` intercepts it, asks the registry which real instance(s) currently answer to `spring-interview-lab`, and rewrites the request to one of them. This is also where client-side load balancing happens for free: if multiple instances are registered under the same name, requests are spread across them without the caller ever knowing.

------------------------------------------------------------------------

## 4. Externalized Configuration — Config Server

Config Server serves `application.properties`/`.yml` content over HTTP from a central place (usually a git repo), instead of every service shipping its own copy baked into the jar. Change a value centrally, and every subscribed service can pick it up **without a redeploy**.

    Config Server                       This App
    (serves properties over HTTP)   ───▶ spring.config.import=configserver:...
         ↑
    backed by a git repo / native filesystem

## Code — `application.properties`

``` properties
spring.config.import=optional:configserver:http://localhost:8888
```

The `optional:` prefix is doing real work here: without it, a missing Config Server would **fail application startup entirely**. With it, Spring Boot logs a connection warning and falls back to local properties — exactly the behavior you saw in the boot log when testing this section with no Config Server running.

------------------------------------------------------------------------

## 5. `@RefreshScope`

By default, `@Value`-injected fields are read once at startup and frozen for the bean's lifetime — even if the underlying config changes. `@RefreshScope` marks a bean as *not* frozen: calling the `/actuator/refresh` endpoint tears it down and rebuilds it, re-reading `@Value` fields against whatever the current config source now says.

## Code — `GreetingProperties.java`

``` java
@RefreshScope
@Component
public class GreetingProperties {

    @Value("${lab.greeting:Hello from the default, no-Config-Server value}")
    private String greeting;

    public String getGreeting() { return greeting; }
}
```

**Endpoint:** `GET /cloud/config` returns the current value. With a real Config Server serving `lab.greeting`, changing that value centrally and calling `POST /actuator/refresh` updates what `/cloud/config` returns — without restarting this app.

------------------------------------------------------------------------

## 6. API Gateway

The single entry point clients actually talk to, sitting in front of every backend service. Two jobs:

-   **Routing** — forward `/orders/**` to the Orders service, `/users/**` to this lab, etc., usually resolved dynamically via the same service discovery from §2 (`lb://spring-interview-lab` instead of a hardcoded host).
-   **Cross-cutting concerns, centralized once** — auth, rate limiting, request logging, CORS — instead of every backend service reimplementing them individually.

    Client
      ↓
    API Gateway  ──▶ Config Server   (centralized settings)
      │
      ├──▶ Service Discovery         (where is each service right now?)
      │
      └──▶ lb://spring-interview-lab (this app, resolved dynamically)

------------------------------------------------------------------------

## 7. Why This Lab Doesn't Ship a Gateway

This is itself worth knowing as an interview answer: **Spring Cloud Gateway requires the reactive stack (WebFlux)**, while this lab is a classic Spring MVC (servlet) application using `spring-boot-starter-web`. Mixing `spring-cloud-starter-gateway` into an MVC app in the same module is not a supported combination — WebFlux and MVC auto-configuration actively conflict when both are on the classpath of one app.

In a real system, the Gateway is **always a separate deployable service** — its own `pom.xml`, its own `main()`, usually its own reactive stack — sitting in front of N backend services, each of which can be plain MVC apps like this one. This lab demonstrates the *client-side* half (discovery + config, both compatible with MVC) inside `com.interview.labs.cloud`; a Gateway would be a genuinely separate companion project, not a package inside this one.

------------------------------------------------------------------------

## 8. Running the Full Picture Locally

To see every warning in this section's logs turn into a real registration/config pull instead:

1.  **Eureka Server** — a separate minimal Spring Boot app with `spring-cloud-starter-netflix-eureka-server` and `@EnableEurekaServer`, run on port `8761`.
2.  **Config Server** — a separate minimal Spring Boot app with `spring-cloud-config-server` and `@EnableConfigServer`, run on port `8888`, pointed at a folder or git repo containing a `spring-interview-lab.properties`.
3.  Start this lab normally (`mvn spring-boot:run`) — it now registers with Eureka and pulls from Config Server on boot, all optional/non-blocking either way.

None of this is required to run or read the rest of the lab — every other section works identically with zero Cloud infrastructure running.

------------------------------------------------------------------------

## 9. Cheat Sheet

    Service Discovery (Eureka)
      "who is registered, and where, right now?"
      register on boot → heartbeat → DiscoveryClient / @LoadBalanced RestTemplate

    Externalized Config (Config Server)
      "same config, many services, no redeploy to change it"
      spring.config.import=optional:configserver:... → @RefreshScope + /actuator/refresh

    API Gateway
      "one entry point for clients, cross-cutting concerns centralized once"
      always a separate service — reactive stack, routes via lb://service-name

------------------------------------------------------------------------

## 10. Important Interview Questions

-   What problem does service discovery solve that DNS alone doesn't?
-   What does `@LoadBalanced` actually intercept, and what does it rewrite?
-   What does the `optional:` prefix on `spring.config.import` change about startup behavior?
-   What does `@RefreshScope` do that a plain `@Value` field doesn't?
-   Why can't Spring Cloud Gateway live in the same module as a `spring-boot-starter-web` MVC app?
-   In a Gateway + Eureka + Config Server + N services architecture, which piece would you make a single point of failure to avoid running redundantly, and why?

------------------------------------------------------------------------

## Where This Started

This section's client-side code builds directly on [Spring Security](06-Spring-Security-README.md) — in a real multi-service system, the JWT issued by `/auth/login` there is exactly what a Gateway would validate once, centrally, before a request ever reaches any backend service.
