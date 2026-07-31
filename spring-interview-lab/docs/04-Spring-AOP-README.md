# Spring AOP Interview Notes

> Complete Revision Guide for Java & Spring Boot Interviews

> Every concept below is backed by `LoggingAspect.java` (`com.interview.labs.aop`) in this same lab, applied to every method in `com.interview.labs.jpa.service.*` — run the app, hit an `/employee/...` endpoint, and watch the console.

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
18. Spring Transaction Internals — Startup vs Runtime
19. BeanPostProcessor
20. Reflection
21. TransactionAttributeSource
22. ProxyFactory (Transactions)
23. Self Invocation (Transactional)
24. TransactionInterceptor
25. PlatformTransactionManager
26. Database Connection & DataSource
27. AutoCommit
28. ThreadLocal
29. TransactionSynchronizationManager
30. Runtime Execution Flow
31. Why ThreadLocal.remove()?

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

## Code — see it happen in this lab

`LoggingAspect` advises every method in `com.interview.labs.jpa.service.*`. `EmployeeService.selfInvocationDemo` calls another method on itself:

``` java
public Employee selfInvocationDemo(Long id, Double salary) {
    return this.updateSalaryWithFlush(id, salary); // this.-call — bypasses the AOP proxy
}
```

Compare the console output of these two endpoints:

- `PUT /employee/{id}/salary/flush?salary=90000` → prints `Before`/`After`/`Around` for `updateSalaryWithFlush`.
- `PUT /employee/{id}/salary/self-invocation-demo?salary=90000` → prints `Before`/`After`/`Around` only for `selfInvocationDemo` — **never** for the inner `updateSalaryWithFlush` call, because it never goes through the proxy.

------------------------------------------------------------------------

## 7. Aspect

A class containing cross-cutting concerns.

Example — `LoggingAspect.java` in this lab:

``` java
@Aspect
@Component
public class LoggingAspect {
    // advice methods below
}
```

------------------------------------------------------------------------

## 8. Advice

The action executed by an Aspect.

Types: - @Before - @After - @Around - @AfterReturning - @AfterThrowing

## Code — all five, from `LoggingAspect.java`

``` java
@Before("execution(* com.interview.labs.jpa.service.*.*(..))")
public void before(JoinPoint joinPoint) {
    System.out.println("Before Method : " + joinPoint.getSignature().getName());
}

@After("execution(* com.interview.labs.jpa.service.*.*(..))")
public void after(JoinPoint joinPoint) {
    System.out.println("After Method : " + joinPoint.getSignature().getName());
}

@AfterReturning(pointcut = "execution(* com.interview.labs.jpa.service.*.*(..))", returning = "result")
public void afterReturning(JoinPoint joinPoint, Object result) {
    System.out.println("After Returning : " + joinPoint.getSignature().getName() + " -> " + result);
}

@AfterThrowing(pointcut = "execution(* com.interview.labs.jpa.service.*.*(..))", throwing = "ex")
public void afterThrowing(JoinPoint joinPoint, Exception ex) {
    System.out.println("After Throwing : " + joinPoint.getSignature().getName() + " -> " + ex.getMessage());
}
```

(`@Around` is covered separately below — it needs `ProceedingJoinPoint`, not `JoinPoint`.)

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

## Code — `LoggingAspect.java`

``` java
@Around("execution(* com.interview.labs.jpa.service.*.*(..))")
public Object around(ProceedingJoinPoint pjp) throws Throwable {

    long start = System.currentTimeMillis();

    Object result = pjp.proceed(); // without this call, the target method never runs

    long elapsed = System.currentTimeMillis() - start;

    System.out.println("Around : " + pjp.getSignature().getName() + " took " + elapsed + "ms");

    return result;
}
```

**Endpoint:** any `/employee/...` call — check the console for the `Around : ... took Nms` line.

------------------------------------------------------------------------

## 14. Why Around is Most Powerful

Every other advice type can only observe the method call. `@Around` is the only one that **controls** it, because it receives a `ProceedingJoinPoint` instead of a plain `JoinPoint`:

| Advice | Can do |
|---|---|
| `@Before` / `@After` | Run code before/after — cannot change arguments, return value, or skip the call |
| `@AfterReturning` / `@AfterThrowing` | Inspect the result or exception — still cannot change control flow |
| `@Around` | Everything above, **plus**: skip `proceed()` entirely, change the arguments passed to it, change the returned value, or catch/replace exceptions it throws |

That's why `@Around` is what `@Transactional`, `@Cacheable`, and `@Async` are actually built on — each of them needs to wrap the call (begin transaction → `proceed()` → commit, or check cache → `proceed()` only on a miss), not just react to it.

### Bonus: Common Spring Features Built on AOP

-   @Transactional
-   @Cacheable
-   @Async
-   Logging (this lab's `LoggingAspect`)

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

---

# 18. Spring Transaction Internals — Startup vs Runtime

`@Transactional` (used throughout [Spring Data JPA §34-37](03-Spring-JPA-README.md)) is implemented using the exact proxy mechanism from the sections above. This part traces what actually happens underneath it — Spring's own machinery, not code written in this lab.

Transaction management runs in **two phases**.

## Phase 1: Application Startup (Runs Only Once)

```text
SpringApplication.run()
        │
        ▼
ApplicationContext
        │
        ▼
Component Scan
        │
        ▼
Bean Definitions
        │
        ▼
Bean Creation
        │
        ▼
BeanPostProcessor
        │
        ▼
Reflection
        │
        ▼
Find @Transactional
        │
        ▼
Create TransactionAttribute
        │
        ▼
Store in TransactionAttributeSource
        │
        ▼
ProxyFactory
        │
        ▼
EmployeeServiceProxy
        │
        ▼
IOC Container
```

### Key Points

- BeanPostProcessor executes **only once** during application startup.
- Reflection is used to detect annotations.
- Spring creates **one proxy per bean**.
- The proxy is stored inside the IOC Container.

## Phase 2: Runtime (Every Request)

```text
HTTP Request
      │
      ▼
Controller
      │
      ▼
EmployeeServiceProxy
      │
      ▼
TransactionInterceptor
      │
      ▼
PlatformTransactionManager
      │
      ▼
Business Method
      │
      ▼
Commit / Rollback
```

BeanPostProcessor is **not executed** during runtime.

---

# 19. BeanPostProcessor

BeanPostProcessor is a Spring extension point that allows Spring to inspect every bean after creation.

Responsibilities:

- Inspect bean
- Decide whether enhancement is required
- Return original bean or proxy

Conceptually:

```java
Object postProcess(Object bean){

    if(needsProxy(bean)){
        return proxyFactory.createProxy(bean);
    }

    return bean;
}
```

> This is Spring Framework's own internal post-processor (`InfrastructureAdvisorAutoProxyCreator`) — not something written in this lab. Every `@Service` bean in `com.interview.labs.jpa.service.*` (`EmployeeService`, `DepartmentService`, `AuditLogService`) passes through it once, at startup, because each has at least one `@Transactional` method.

---

# 20. Reflection

Spring uses Java Reflection during startup.

Conceptually:

```java
for(Method method : clazz.getDeclaredMethods()){

    if(method.isAnnotationPresent(Transactional.class)){

    }

}
```

Reflection is performed **once** during startup.

---

# 21. TransactionAttributeSource

Spring does not perform reflection for every request.

Instead it stores metadata.

Example — the audit-log demo from the JPA guide ([§35](03-Spring-JPA-README.md)):

```java
@Transactional(
    propagation = Propagation.REQUIRED,
    isolation = Isolation.READ_COMMITTED,
    rollbackFor = Exception.class
)
public Employee updateSalaryWithAudit(Long id, Double salary, boolean simulateFailure) throws Exception { ... }
```

Metadata stored:

```text
Method : updateSalaryWithAudit()

Propagation : REQUIRED

Isolation : READ_COMMITTED

ReadOnly : false

Rollback : Exception.class (forced via rollbackFor)
```

This metadata is represented by **TransactionAttribute**.

---

# 22. ProxyFactory (Transactions)

If BeanPostProcessor finds that transaction support is required:

```text
EmployeeService
        │
        ▼
ProxyFactory
        │
        ▼
EmployeeServiceProxy
```

Spring creates **one proxy per bean**.

Not one proxy per method.

> The same proxy created for `EmployeeService` here is the one `LoggingAspect` ([§3](#3-spring-proxy)) also advises — a bean gets exactly one proxy that all applicable aspects (transactions, logging, whatever else applies) share, not a separate proxy per concern.

---

# 23. Self Invocation (Transactional)

```java
@Transactional
public void save(){

    helper();

}
```

Execution:

```text
Proxy

↓

Real save()

↓

Real helper()
```

Internal method calls (`this.helper()`) bypass the proxy.

Therefore:

- No Transaction
- No AOP Advice

## Code — see it happen in this lab

This is the exact same failure mode as [§6 Self Invocation](#6-self-invocation) for logging, demonstrated with the transactional method itself:

```java
public Employee selfInvocationDemo(Long id, Double salary) {
    return this.updateSalaryWithFlush(id, salary); // this.-call — bypasses the proxy
}
```

`updateSalaryWithFlush` is `@Transactional`. Called directly (`PUT /employee/{id}/salary/flush`), it runs inside a real transaction. Called through `selfInvocationDemo` (`PUT /employee/{id}/salary/self-invocation-demo`), the inner call runs with **no transaction at all** — `this.` never touches `EmployeeServiceProxy`.

---

# 24. TransactionInterceptor

The proxy delegates transaction handling to TransactionInterceptor.

Conceptually:

```java
invoke(){

    beginTransaction();

    try{

        businessMethod();

        commit();

    }catch(Exception e){

        rollback();

    }

}
```

Responsibilities:

- Read transaction metadata
- Begin transaction
- Commit
- Rollback

---

# 25. PlatformTransactionManager

TransactionInterceptor delegates transaction operations to PlatformTransactionManager.

Responsibilities:

- Obtain database connection
- Disable AutoCommit
- Bind connection to current thread
- Commit transaction
- Rollback transaction

---

# 26. Database Connection & DataSource

Spring never creates Connection directly.

Instead:

```java
Connection connection =
        dataSource.getConnection();
```

Usually DataSource is backed by **HikariCP**.

```text
Spring

↓

DataSource

↓

HikariCP

↓

Connection
```

> This lab never configures a connection pool explicitly (`pom.xml` and `application.properties` have no HikariCP settings), so Spring Boot's auto-configured default — HikariCP, pulled in transitively via `spring-boot-starter-data-jpa` — is exactly what's serving every `@Transactional` method here.

---

# 27. AutoCommit

Default JDBC

```java
connection.getAutoCommit();
```

returns

```text
true
```

Meaning every SQL is committed immediately.

Spring executes:

```java
connection.setAutoCommit(false);
```

Reason:

- Multiple SQL statements
- Single transaction
- Rollback support

---

# 28. ThreadLocal

Without ThreadLocal

```java
repository.save(connection);

dao.save(connection);

jdbc.save(connection);
```

Connection must be passed everywhere.

With ThreadLocal

```java
threadLocal.set(connection);
```

Later

```java
threadLocal.get();
```

Same connection is available throughout the request.

## Code — see it happen in this lab

```java
@Transactional
public Employee updateSalaryWithFlush(Long id, Double salary) {

    Employee employee = repository.findById(id)   // uses the ThreadLocal connection
            .orElseThrow(() -> new RuntimeException("Employee Not Found"));

    employee.setSalary(salary);
    entityManager.flush();                          // same ThreadLocal connection

    return employee;
}
```

`repository.findById(...)` and `entityManager.flush()` never pass a `Connection` to each other explicitly — both pull the same bound connection off the current thread.

---

# 29. TransactionSynchronizationManager

Spring stores transaction resources using TransactionSynchronizationManager.

Responsibilities:

- Bind Connection
- Retrieve Connection
- Remove Connection

Internally uses:

```text
ThreadLocal
```

---

# 30. Runtime Execution Flow

```text
Controller
      │
      ▼
EmployeeServiceProxy
      │
      ▼
TransactionInterceptor
      │
      ▼
PlatformTransactionManager.begin()
      │
      ▼
DataSource.getConnection()
      │
      ▼
HikariCP
      │
      ▼
Connection
      │
      ▼
connection.setAutoCommit(false)
      │
      ▼
TransactionSynchronizationManager.bindResource()
      │
      ▼
ThreadLocal.set(Connection)
      │
      ▼
repository.save(emp1)
      │
      ▼
ThreadLocal.get()
      │
      ▼
Connection
      │
      ▼
repository.save(emp2)
      │
      ▼
ThreadLocal.get()
      │
      ▼
Connection
      │
      ▼
Commit / Rollback
      │
      ▼
TransactionSynchronizationManager.unbindResource()
      │
      ▼
ThreadLocal.remove()
```

---

# 31. Why ThreadLocal.remove()?

Tomcat uses a thread pool.

Threads are reused.

If ThreadLocal is not cleared:

- Stale Connection may be reused.
- Wrong transaction context.
- Memory/resource leak.

Therefore Spring always executes:

```java
threadLocal.remove();
```

---

## Startup vs Runtime — Quick Recap

**Startup** (runs once): `BeanPostProcessor → Reflection → TransactionAttributeSource → ProxyFactory → IOC Container`

**Runtime** (every request): `Proxy → TransactionInterceptor → PlatformTransactionManager → Business Logic → Commit / Rollback`

## ⭐ More Interview Questions — Transaction Internals

- Why does `BeanPostProcessor` only run at startup, not per request?
- Why does Spring cache `@Transactional` metadata (`TransactionAttributeSource`) instead of re-reading annotations via reflection on every call?
- Why exactly one proxy per bean, not one per method?
- Why does self-invocation defeat `@Transactional` the same way it defeats `@Before`/`@After` logging?
- Where does the JDBC `Connection` actually come from, and why is `setAutoCommit(false)` required?
- How do `repository.save(...)` calls scattered across a transactional method all use the *same* connection without passing it explicitly?
- Why must `ThreadLocal.remove()` run at the end of every request on a pooled-thread server like Tomcat?

## Revision Flow

```text
Application Startup

↓

BeanPostProcessor

↓

Reflection

↓

@Transactional Found

↓

TransactionAttribute

↓

TransactionAttributeSource

↓

ProxyFactory

↓

Proxy Stored in IOC

=============================

Runtime

↓

Controller

↓

Proxy

↓

TransactionInterceptor

↓

PlatformTransactionManager

↓

DataSource

↓

HikariCP

↓

Connection

↓

setAutoCommit(false)

↓

TransactionSynchronizationManager

↓

ThreadLocal

↓

Business Logic

↓

Commit / Rollback

↓

ThreadLocal.remove()
```
