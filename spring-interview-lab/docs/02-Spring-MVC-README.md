# Spring MVC Interview Notes

---

# 1. What is Spring MVC?

## Definition

Spring MVC is a web framework built on the **Model-View-Controller (MVC)** design pattern. It separates an application into three layers: **Model**, **View**, and **Controller**, making applications easier to develop, test, and maintain.

---

## MVC Components

### Model

Contains:

* Business Logic
* Service Layer
* Repository Layer
* Database Operations

---

### View

Represents the user interface.

Examples:

* JSP
* Thymeleaf
* HTML
* JSON (REST APIs)

In Spring Boot REST applications, the response is typically **JSON**.

---

### Controller

The Controller receives HTTP requests, delegates them to the Service layer, and returns the response.

---

## Spring MVC Architecture

```text
Browser
    │
    ▼
DispatcherServlet
    │
    ▼
HandlerMapping
    │
    ▼
HandlerAdapter
    │
    ▼
Controller
    │
    ▼
Service
    │
    ▼
Repository
    │
    ▼
Database
    │
    ▼
Controller
    │
    ▼
HttpMessageConverter
    │
    ▼
JSON Response
    │
    ▼
Browser
```

---

## Advantages of Spring MVC

* Separation of Concerns
* Easy Testing
* Easy Maintenance
* REST API Support
* Validation
* Exception Handling
* Flexible Request Mapping

---

## Real Project Example

```text
Angular Client
        │
POST /applications
        │
DispatcherServlet
        │
ApplicationController
        │
ApplicationService
        │
ApplicationRepository
        │
Oracle Database
        │
JSON Response
        │
Angular Client
```

---

# 2. DispatcherServlet

## Definition

DispatcherServlet is the **Front Controller** of Spring MVC.

It receives every incoming HTTP request and coordinates the complete request-processing lifecycle.

---

## Responsibilities

* Receives all HTTP requests
* Delegates requests to the correct controller
* Coordinates request processing
* Uses HandlerMapping to locate handlers
* Uses HandlerAdapter to invoke controller methods
* Coordinates request/response conversion
* Handles exceptions

> **DispatcherServlet does not contain business logic.**

---

## Internal Flow

```text
Client
    │
    ▼
DispatcherServlet
    │
    ▼
HandlerMapping
    │
    ▼
HandlerAdapter
    │
    ▼
Controller
    │
    ▼
Service
    │
    ▼
Repository
    │
    ▼
Database
    │
    ▼
HttpMessageConverter
    │
    ▼
JSON Response
```

---

# 3. HandlerMapping

## Definition

HandlerMapping identifies the correct controller method based on the HTTP request.

---

## Internal Working

During application startup:

* Spring scans all controllers.
* Finds `@GetMapping`, `@PostMapping`, etc.
* Creates an internal mapping registry.

Example:

| HTTP Method | URL           | Controller Method |
| ----------- | ------------- | ----------------- |
| GET         | `/users/{id}` | `getUser()`       |
| POST        | `/users`      | `saveUser()`      |

When a request arrives:

```text
GET /users/101

↓

HandlerMapping

↓

Matches GET + /users/{id}

↓

Returns getUser()
```

---

# 4. HandlerAdapter

## Definition

HandlerAdapter invokes the controller method selected by HandlerMapping.

Responsibilities:

* Invoke controller methods
* Pass method arguments
* Receive return values
* Return control to DispatcherServlet

---

# 5. Controller vs RestController

## @Controller

Used for traditional Spring MVC applications.

Returns:

* JSP
* Thymeleaf
* HTML

Example:

```java
@Controller
public class HomeController {

    @GetMapping("/home")
    public String home() {
        return "home";
    }
}
```

---

## @RestController

Used for REST APIs.

Returns Java objects that Spring converts to JSON.

Internally:

```text
@RestController
      =
@Controller
+
@ResponseBody
```

Example:

```java
@RestController
public class UserController {

    @GetMapping("/user")
    public User getUser() {
        return new User();
    }
}
```

---

## Comparison

| @Controller                  | @RestController        |
| ---------------------------- | ---------------------- |
| Returns View                 | Returns JSON/XML       |
| Used for JSP/Thymeleaf       | Used for REST APIs     |
| Needs @ResponseBody for JSON | @ResponseBody included |

---

# 6. @RequestBody

## Definition

`@RequestBody` binds the HTTP request body (typically JSON) to a Java object.

Example:

```java
@PostMapping("/users")
public User saveUser(@RequestBody User user) {
    return userService.save(user);
}
```

---

## Internal Working

```text
Client
    │
JSON Request
    │
DispatcherServlet
    │
HttpMessageConverter
    │
Jackson ObjectMapper
    │
Deserialize JSON
    │
Java Object
    │
Controller
```

---

## Response Flow

```text
Java Object
    │
ObjectMapper
    │
Serialize
    │
JSON
    │
HTTP Response
```

---

# 7. @PathVariable vs @RequestParam

## @PathVariable

Extracts values from the URL path.

Example:

```http
GET /users/101
```

```java
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id)
```

Used for identifying a specific resource.

---

## @RequestParam

Extracts values from query parameters.

Example:

```http
GET /users?id=101
```

```java
@GetMapping("/users")
public User getUser(@RequestParam Long id)
```

Used for:

* Filtering
* Searching
* Pagination
* Sorting

---

## Comparison

| @PathVariable           | @RequestParam    |
| ----------------------- | ---------------- |
| URL Path                | Query Parameter  |
| Mandatory               | Can be optional  |
| Resource Identification | Filtering/Search |

---

# 8. @RequestBody vs @ModelAttribute

## @RequestBody

* JSON/XML
* Uses HttpMessageConverter
* Uses Jackson ObjectMapper
* Common in REST APIs

---

## @ModelAttribute

* HTML Form Data
* Uses Spring Data Binder
* No Jackson
* Common in JSP/Thymeleaf applications

---

## Comparison

| @RequestBody         | @ModelAttribute |
| -------------------- | --------------- |
| JSON/XML             | Form Data       |
| HttpMessageConverter | Data Binder     |
| REST APIs            | Traditional MVC |

---

# 9. HttpMessageConverter

## Definition

HttpMessageConverter converts:

* HTTP Request → Java Object
* Java Object → HTTP Response

For JSON, Spring uses Jackson's ObjectMapper.

---

## Request

```text
JSON
    │
ObjectMapper
    │
Deserialize
    │
Java Object
```

---

## Response

```text
Java Object
    │
ObjectMapper
    │
Serialize
    │
JSON
```

---

# 10. Filter vs Interceptor

## Filter

Part of the Servlet API.

Runs **before DispatcherServlet**.

Common Uses:

* Authentication
* CORS
* Logging
* Compression

---

## Interceptor

Part of Spring MVC.

Runs **after DispatcherServlet but before the Controller**.

Methods:

* `preHandle()`
* `postHandle()`
* `afterCompletion()`

Common Uses:

* Logging
* Authorization
* Performance Monitoring
* Auditing

---

## Request Flow

```text
Client
    │
Filter
    │
DispatcherServlet
    │
Interceptor
    │
Controller
    │
Service
    │
Repository
    │
Database
    │
Response
```

---

## Comparison

| Filter                   | Interceptor                      |
| ------------------------ | -------------------------------- |
| Servlet API              | Spring MVC                       |
| Before DispatcherServlet | After DispatcherServlet          |
| All Servlet Requests     | Spring MVC Requests              |
| Authentication, CORS     | Logging, Authorization, Auditing |

---

# JWT Authentication

In Spring Security, JWT validation is typically performed in a custom filter that extends `OncePerRequestFilter`.

Typical flow:

```text
HTTP Request
      │
Authorization Header
      │
Bearer Token
      │
JwtAuthenticationFilter
      │
Validate Token
      │
Create Authentication
      │
SecurityContextHolder
      │
filterChain.doFilter()
      │
DispatcherServlet
      │
Controller
```

This ensures authentication is completed before the request reaches the controller.

---

# Common Spring MVC Interview Questions

* What is Spring MVC?
* Explain MVC architecture.
* What is DispatcherServlet?
* Explain the complete Spring MVC request flow.
* What is HandlerMapping?
* What is HandlerAdapter?
* @Controller vs @RestController?
* @RequestBody vs @ModelAttribute?
* @PathVariable vs @RequestParam?
* What is HttpMessageConverter?
* How does Spring convert JSON to Java objects?
* Filter vs Interceptor?
* Why is JWT validation done in a Filter?
* What is OncePerRequestFilter?

---

# Spring MVC Cheat Sheet

```text
Browser
      │
      ▼
DispatcherServlet
      │
      ▼
HandlerMapping
      │
      ▼
HandlerAdapter
      │
      ▼
Controller
      │
      ▼
HttpMessageConverter
      │
      ▼
Service
      │
      ▼
Repository
      │
      ▼
Database
      │
      ▼
HttpMessageConverter
      │
      ▼
JSON Response
```
