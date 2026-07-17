# Spring AOP Interview Notes

> Complete Revision Guide for Java & Spring Boot Interviews

## Table of Contents

1.  What is AOP?
2.  Cross Cutting Concern
3.  Spring Proxy
4.  JDK Dynamic Proxy
5.  CGLIB Proxy
6.  Self Invocation
7.  Aspect
8.  Advice
9.  Join Point
10. Pointcut
11. Weaving
12. Types of Advice
13. ProceedingJoinPoint
14. Why Around is Most Powerful
15. Spring AOP Flow
16. Cheat Sheet
17. Important Interview Questions

------------------------------------------------------------------------

## 1. What is AOP?

AOP (Aspect-Oriented Programming) is a programming paradigm used to
separate **cross-cutting concerns** from **business logic**.

Common cross-cutting concerns:

-   Logging
-   Security
-   Transactions
-   Caching
-   Exception Handling

### Without AOP

    Logging
      ↓
    Security
      ↓
    Transaction
      ↓
    Business Logic
      ↓
    Logging

### With AOP

    Business Logic
          ↓
      Spring AOP
          ↓
    Logging / Security / Transaction

Architecture:

    Client
      ↓
    Spring Proxy
      ↓
    Aspect
      ↓
    Business Method
      ↓
    Database

------------------------------------------------------------------------

## 2. Cross Cutting Concern

Common functionalities used across multiple modules:

-   Logging
-   Security
-   Transactions
-   Caching
-   Performance Monitoring
-   Exception Handling

------------------------------------------------------------------------

## 3. Spring Proxy

A proxy is an object created by Spring that sits between the client and
the target object.

Flow:

    Client
      ↓
    Proxy
      ↓
    EmployeeService
      ↓
    Database

Responsibilities:

-   Logging
-   Transaction Management
-   Security
-   Caching
-   Performance Monitoring

------------------------------------------------------------------------

## 4. JDK Dynamic Proxy

-   Used when target implements an interface.
-   Uses `java.lang.reflect.Proxy`.

Flow:

    Interface
      ↓
    JDK Proxy
      ↓
    Implementation

Advantages: - Fast - Built into Java

Limitation: - Works only with interfaces.

------------------------------------------------------------------------

## 5. CGLIB Proxy

-   Used when no interface exists.
-   Creates proxy by extending the target class.

Limitations: - Cannot proxy final classes - Cannot proxy final methods

JDK Proxy              CGLIB
  ---------------------- ---------------
Interface Based        Class Based
Uses Reflection        Uses Bytecode
Implements Interface   Extends Class

------------------------------------------------------------------------

## 6. Self Invocation

Calling one method from another within the same class.

``` java
public void save() {
    find();
}

@Transactional
public void find() {
}
```

Actual call:

``` java
this.find();
```

Result: - No Transaction - No Logging - No Cache - No Security

Solution: - Move the transactional method to another Spring Bean.

------------------------------------------------------------------------

## 7. Aspect

A class containing cross-cutting concerns.

Example:

``` java
@Aspect
@Component
public class LoggingAspect {
}
```

------------------------------------------------------------------------

## 8. Advice

The action executed by an Aspect.

Types: - @Before - @After - @Around - @AfterReturning - @AfterThrowing

------------------------------------------------------------------------

## 9. Join Point

A point where Advice can be applied.

In Spring AOP:

**Join Point = Method Execution**

------------------------------------------------------------------------

## 10. Pointcut

An expression selecting which Join Points execute the Advice.

Example:

``` java
execution(* com.company.service.*.*(..))
```

------------------------------------------------------------------------

## 11. Weaving

The process of applying an Aspect to the target object using a Spring
Proxy.

------------------------------------------------------------------------

## 12. Types of Advice

-   **@Before** -- Before method execution
-   **@After** -- After method execution (success or exception)
-   **@AfterReturning** -- After successful execution
-   **@AfterThrowing** -- After exception
-   **@Around** -- Before and after; can control execution

Execution Order:

    Around Before
          ↓
      @Before
          ↓
    Business Method
          ↓
      @After
          ↓
    @AfterReturning
          ↓
    Around After

------------------------------------------------------------------------

## 13. ProceedingJoinPoint

Available only in `@Around`.

Important API:

``` java
pjp.proceed();
```

Without `proceed()`, the target method never executes.

Capabilities: - Execute before - Execute after - Skip execution - Modify
arguments - Modify return value

------------------------------------------------------------------------

## 14. Common Spring Features Using AOP

-   @Transactional
-   @Cacheable
-   @Async
-   Logging

------------------------------------------------------------------------

## 15. Spring AOP Flow

    Client
      ↓
    Proxy
      ↓
    Pointcut
      ↓
    Advice
      ↓
    Business Method
      ↓
    Response

------------------------------------------------------------------------

## 16. Cheat Sheet

    Cross Cutting Concern
            ↓
         Aspect
            ↓
         Advice
            ↓
       Join Point
            ↓
        Pointcut
            ↓
         Weaving
            ↓
      Spring Proxy

------------------------------------------------------------------------

## 17. Important Interview Questions

-   What is AOP?
-   What is an Aspect?
-   What is Advice?
-   What is Join Point?
-   What is Pointcut?
-   What is Weaving?
-   Difference between Join Point and Pointcut.
-   Difference between JDK Proxy and CGLIB.
-   Why does Self Invocation fail?
-   Why does @Around use ProceedingJoinPoint?

### Memory Trick

    Business Logic
          │
          ▼
    Spring Proxy
          │
          ▼
    Pointcut Match?
          │
         YES
          │
          ▼
    Advice Executes
          │
          ▼
    Business Method
          │
          ▼
       Response
