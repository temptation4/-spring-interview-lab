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
