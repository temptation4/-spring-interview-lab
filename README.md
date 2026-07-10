# Spring Core Interview Notes

---

# 1. Spring Framework

## Definition

Spring is a lightweight, open-source Java framework used to build enterprise applications. It promotes **Loose Coupling**, **Dependency Injection (DI)**, and **Inversion of Control (IoC)**, making applications easier to develop, test, and maintain.

## Why Spring?

Without Spring:

* Objects create their own dependencies using `new`
* Tight coupling
* Difficult to test
* Difficult to maintain

With Spring:

* Spring creates and manages objects (Beans)
* Dependencies are injected automatically
* Loose coupling
* Better maintainability and testability

---

# 2. Spring Architecture

```
Spring Framework
│
├── Core Container
├── AOP
├── Data Access
├── MVC
├── Security
├── Transaction Management
└── Testing
```

### Core Container

* Core
* Beans
* Context
* Expression Language (SpEL)

The Core Container provides the IoC Container and Dependency Injection.

---

# 3. Inversion of Control (IoC)

## Definition

IoC (Inversion of Control) is a design principle in which the responsibility of creating, configuring, and managing objects is transferred from the application code to the Spring IoC Container.

Instead of writing:

```java
PaymentService paymentService = new PaymentService();
```

Spring creates the object for us.

## Why IoC?

* Loose Coupling
* Easy Testing
* Better Maintainability
* Centralized Object Management

---

# 4. Dependency Injection (DI)

## Definition

Dependency Injection is the mechanism used by the Spring IoC Container to provide required dependencies to a bean instead of the bean creating them itself.

## IoC vs DI

| IoC                             | DI                          |
| ------------------------------- | --------------------------- |
| Design Principle                | Mechanism                   |
| Spring controls object creation | Spring injects dependencies |
| High-level concept              | Implementation of IoC       |

---

# 5. Types of Dependency Injection

## Constructor Injection (Recommended)

```java
@Service
public class OrderService {

    private final PaymentService paymentService;

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

### Advantages

* Mandatory dependencies
* Immutable object
* Easy unit testing
* Recommended by Spring
* Detects circular dependency early

---

## Setter Injection

```java
@Service
public class OrderService {

    private PaymentService paymentService;

    @Autowired
    public void setPaymentService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

### Advantages

* Optional dependency
* Dependency can be changed later

### Disadvantages

* Object may exist without required dependency
* Mutable object

---

## Field Injection

```java
@Service
public class OrderService {

    @Autowired
    private PaymentService paymentService;
}
```

### Advantages

* Less code
* Easy to write

### Disadvantages

* Hidden dependency
* Difficult to unit test
* Uses reflection
* Not immutable

---

# Why Constructor Injection is Preferred?

| Constructor            | Field               |
| ---------------------- | ------------------- |
| Mandatory dependencies | Hidden dependencies |
| Immutable              | Mutable             |
| Easy testing           | Difficult testing   |
| Recommended by Spring  | Not recommended     |

---

# 6. Spring Bean

## Definition

A Spring Bean is an object that is created, configured, initialized, and managed by the Spring IoC Container.

A normal Java object becomes a Spring Bean when it is:

* Discovered during Component Scanning (`@Component`, `@Service`, `@Repository`, `@Controller`)
* Created using an `@Bean` method inside a `@Configuration` class

---

# 7. Bean Definition

## Definition

A BeanDefinition is metadata about a Spring Bean.

It contains:

* Bean Name
* Bean Class
* Bean Scope
* Constructor Information
* Dependencies
* Init Method
* Destroy Method
* Lazy Initialization
* Primary Bean Information

Spring creates BeanDefinitions before creating actual objects.

## Why BeanDefinition?

Spring first collects metadata for all beans.

After collecting metadata, Spring:

* Resolves dependencies
* Determines bean creation order
* Creates objects
* Applies lifecycle callbacks

---

# 8. BeanFactory

## Definition

BeanFactory is the basic IoC Container.

### Responsibilities

* Create Beans
* Manage Bean Lifecycle
* Perform Dependency Injection

Beans are generally created lazily.

---

# 9. ApplicationContext

ApplicationContext extends BeanFactory.

It provides:

* AOP Support
* Event Publishing
* Resource Loading
* Internationalization
* Environment & Profiles
* BeanPostProcessor Support

Spring Boot uses ApplicationContext.

---

# BeanFactory vs ApplicationContext

| BeanFactory         | ApplicationContext                      |
| ------------------- | --------------------------------------- |
| Basic IoC Container | Advanced IoC Container                  |
| Lazy Loading        | Supports eager singleton initialization |
| No Event Support    | Event Publishing                        |
| No AOP Support      | AOP Support                             |
| Rarely used         | Used by Spring Boot                     |

---

# 10. Spring Boot Startup Flow

```
JVM Starts
        │
        ▼
main()
        │
        ▼
SpringApplication.run()
        │
        ▼
Create ApplicationContext
        │
        ▼
Read @SpringBootApplication
        │
        ▼
Component Scan
        │
        ▼
Create BeanDefinitions
        │
        ▼
Auto Configuration
        │
        ▼
Instantiate Beans
        │
        ▼
Dependency Injection
        │
        ▼
@PostConstruct
        │
        ▼
Embedded Tomcat Starts
        │
        ▼
Application Ready
```

---

# 11. How @Autowired Works Internally

```
Component Scan
        │
        ▼
Create BeanDefinition
        │
        ▼
Instantiate Bean
        │
        ▼
Find @Autowired
        │
        ▼
Search Dependency
        │
        ▼
Reflection
        │
        ▼
Inject Dependency
        │
        ▼
@PostConstruct
        │
        ▼
Bean Ready
```

Spring uses Reflection internally.

Example:

```java
Field field = OrderService.class.getDeclaredField("paymentService");

field.setAccessible(true);

field.set(orderService, paymentServiceBean);
```

Reflection allows Spring to inject values into private fields.

---

# 12. Reflection Usage in Spring

Spring uses Reflection for:

* Bean Instantiation
* Dependency Injection
* @Autowired
* @Value
* @PostConstruct
* Proxy Creation

---

# 13. Bean Scopes

## Singleton (Default)

* One object per Spring Container
* Stored and reused

## Prototype

* New object every request from the container
* Spring creates it but does not manage its destruction

## Request

One bean per HTTP request.

## Session

One bean per HTTP session.

## Application

One bean per web application.

---

# Singleton vs Prototype

| Singleton                     | Prototype                                                 |
| ----------------------------- | --------------------------------------------------------- |
| One object                    | New object each container request                         |
| Cached                        | Not shared as a singleton                                 |
| Spring manages full lifecycle | Spring creates and injects; caller manages after creation |

---

# 14. Circular Dependency

Example

```java
@Service
class A {

    @Autowired
    private B b;
}

@Service
class B {

    @Autowired
    private A a;
}
```

```
A needs B
      ↓
B needs A
```

This is called Circular Dependency.

## Constructor Injection

```
A(B)

B(A)
```

Spring cannot create either bean first.

Startup fails.

## Best Solution

Refactor the design.

Alternative solutions:

* `@Lazy`
* Setter Injection (with circular references enabled, not recommended as a design choice)

---

# 15. @Primary vs @Qualifier

## @Primary

Marks the default bean.

```java
@Service
@Primary
class PhonePeService implements PaymentService {
}
```

---

## @Qualifier

Selects a specific bean.

```java
@Autowired
@Qualifier("phonePeService")
private PaymentService paymentService;
```

---

## Comparison

| @Primary     | @Qualifier         |
| ------------ | ------------------ |
| Default Bean | Specific Bean      |
| Bean Level   | Injection Point    |
| Automatic    | Explicit Selection |

---

# 16. Common Interview Questions

### Spring Framework

* What is Spring?
* Why Spring?
* Explain Spring Architecture.

### IoC & DI

* What is IoC?
* What is Dependency Injection?
* IoC vs DI?
* Types of DI?
* Why Constructor Injection?

### Bean

* What is a Spring Bean?
* Bean vs Object?
* Bean vs BeanDefinition?
* Bean Lifecycle?
* Bean Scopes?

### Spring Container

* BeanFactory vs ApplicationContext?
* Why does Spring Boot use ApplicationContext?

### Startup

* Explain Spring Boot Startup Flow.
* What happens inside `SpringApplication.run()`?

### Dependency Injection

* How does `@Autowired` work internally?
* How does Spring inject private fields?
* Why does Spring use Reflection?

### Circular Dependency

* What is Circular Dependency?
* Why does Constructor Injection fail?
* How can you solve it?

### Bean Selection

* What is `@Primary`?
* What is `@Qualifier`?
* When would you use each?

---

# 17. Spring Core Cheat Sheet

```
SpringApplication.run()
        │
        ▼
ApplicationContext
        │
        ▼
Component Scan
        │
        ▼
BeanDefinition
        │
        ▼
Instantiate Bean
        │
        ▼
Dependency Injection
        │
        ▼
@PostConstruct
        │
        ▼
Application Ready
```

### Interview Keywords

* IoC
* Dependency Injection
* Bean
* BeanDefinition
* BeanFactory
* ApplicationContext
* Reflection
* Component Scan
* Constructor Injection
* @Autowired
* Bean Scope
* Circular Dependency
* @Primary
* @Qualifier

  # Spring MVC Interview Notes

---

#  What is Spring MVC?

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

