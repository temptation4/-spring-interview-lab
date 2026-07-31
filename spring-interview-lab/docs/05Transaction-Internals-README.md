# Spring Transaction Internals

> How `@Transactional` (used throughout [Spring Data JPA §34-37](../../docs/03-Spring-JPA-README.md)) is actually implemented — the same AOP proxy mechanism from the [Spring AOP guide](../../docs/04-Spring-AOP-README.md), applied specifically to transactions.

Real, runnable code for the parts of this that make sense to hand-write for teaching lives in `com.interview.labs.transaction`:

- [`TransactionAwareBeanPostProcessor.java`](../../src/main/java/com/interview/labs/transaction/TransactionAwareBeanPostProcessor.java) — a real `BeanPostProcessor` that reflects over every bean at startup looking for `@Transactional`
- [`TransactionContextHolder.java`](../../src/main/java/com/interview/labs/transaction/TransactionContextHolder.java) — a hand-rolled `ThreadLocal`, wired into `LoggingAspect`

`BeanPostProcessor`'s actual transaction-proxy creation, `TransactionInterceptor`, and `PlatformTransactionManager` are Spring Framework's own internals — not reimplemented here, just traced.

## Table of Contents

1. Startup vs Runtime
2. BeanPostProcessor
3. Reflection
4. TransactionAttributeSource
5. ProxyFactory (Transactions)
6. Self Invocation (Transactional)
7. TransactionInterceptor
8. PlatformTransactionManager
9. Database Connection & DataSource
10. AutoCommit
11. ThreadLocal
12. TransactionSynchronizationManager
13. Runtime Execution Flow
14. Why ThreadLocal.remove()?
15. Why @Transactional Doesn't Work on Private Methods
16. Declarative vs Programmatic Transactions
17. readOnly and timeout
18. NESTED vs REQUIRES_NEW
19. Transaction Synchronization Callbacks

---

# 1. Startup vs Runtime

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

# 2. BeanPostProcessor

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

> Spring's real transaction-proxy `BeanPostProcessor` (`InfrastructureAdvisorAutoProxyCreator`) is internal — not something written in this lab. Every `@Service` bean in `com.interview.labs.jpa.service.*` (`EmployeeService`, `DepartmentService`, `AuditLogService`) passes through it once, at startup, because each has at least one `@Transactional` method.

## Code — a teaching BeanPostProcessor in this lab

Spring's real one can't be inspected directly, so `TransactionAwareBeanPostProcessor` does the same *inspection* step — reflection over every bean, looking for `@Transactional` — as a small, real, runnable `BeanPostProcessor`:

```java
@Component
public class TransactionAwareBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        // If another BeanPostProcessor already wrapped this bean in a CGLIB proxy,
        // the @Transactional-annotated methods live on the superclass.
        Class<?> targetClass = bean.getClass();
        if (targetClass.getName().contains("$$")) {
            targetClass = targetClass.getSuperclass();
        }

        List<String> transactionalMethods = Arrays.stream(targetClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Transactional.class))
                .map(Method::getName)
                .collect(Collectors.toList());

        if (!transactionalMethods.isEmpty()) {
            System.out.println("[TransactionAwareBeanPostProcessor] " + beanName
                    + " has @Transactional methods: " + transactionalMethods);
        }

        return bean;
    }
}
```

Run the app — the console prints this at startup for `EmployeeService`, `DepartmentService`, and `AuditLogService`.

---

# 3. Reflection

Spring uses Java Reflection during startup.

Conceptually:

```java
for(Method method : clazz.getDeclaredMethods()){

    if(method.isAnnotationPresent(Transactional.class)){

    }

}
```

Reflection is performed **once** during startup — see the real version above, in `TransactionAwareBeanPostProcessor`.

---

# 4. TransactionAttributeSource

Spring does not perform reflection for every request.

Instead it stores metadata.

Example — the audit-log demo from the JPA guide ([§35](../../docs/03-Spring-JPA-README.md)):

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

# 5. ProxyFactory (Transactions)

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

> The same proxy created for `EmployeeService` here is the one `LoggingAspect` ([Spring AOP §3](../../docs/04-Spring-AOP-README.md#3-spring-proxy)) also advises — a bean gets exactly one proxy that all applicable aspects (transactions, logging, whatever else applies) share, not a separate proxy per concern.

---

# 6. Self Invocation (Transactional)

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

This is the exact same failure mode as [Spring AOP §6 Self Invocation](../../docs/04-Spring-AOP-README.md#6-self-invocation), demonstrated with the transactional method itself:

```java
public Employee selfInvocationDemo(Long id, Double salary) {
    return this.updateSalaryWithFlush(id, salary); // this.-call — bypasses the proxy
}
```

`updateSalaryWithFlush` is `@Transactional`. Called directly (`PUT /employee/{id}/salary/flush`), it runs inside a real transaction. Called through `selfInvocationDemo` (`PUT /employee/{id}/salary/self-invocation-demo`), the inner call runs with **no transaction at all** — `this.` never touches `EmployeeServiceProxy`.

---

# 7. TransactionInterceptor

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

# 8. PlatformTransactionManager

TransactionInterceptor delegates transaction operations to PlatformTransactionManager.

Responsibilities:

- Obtain database connection
- Disable AutoCommit
- Bind connection to current thread
- Commit transaction
- Rollback transaction

---

# 9. Database Connection & DataSource

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

# 10. AutoCommit

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

# 11. ThreadLocal

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

## Code — Spring's version, seen through this lab

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

## Code — a hand-rolled ThreadLocal, wired into LoggingAspect

`TransactionContextHolder` is the same technique in miniature — a value bound to the current thread instead of a database connection:

```java
public final class TransactionContextHolder {
    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();
    public static void set(String correlationId) { CONTEXT.set(correlationId); }
    public static String get() { return CONTEXT.get(); }
    public static void clear() { CONTEXT.remove(); }
}
```

`LoggingAspect.around()` sets a correlation id on it before `proceed()` and clears it in `finally` — `before()`/`after()` read it back without it ever being passed as a method argument:

```java
@Around("execution(* com.interview.labs.jpa.service.*.*(..))")
public Object around(ProceedingJoinPoint pjp) throws Throwable {
    TransactionContextHolder.set(UUID.randomUUID().toString().substring(0, 8));
    try {
        return pjp.proceed();
    } finally {
        TransactionContextHolder.clear(); // always — even on exception
    }
}
```

Hit any `/employee/...` endpoint and the console's `Before Method [xxxxxxxx] : ...` line shows the same id across `before()`/`after()` for that one call, and a *different* id on the next call — proof it's thread-scoped, not shared global state.

---

# 12. TransactionSynchronizationManager

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

# 13. Runtime Execution Flow

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

# 14. Why ThreadLocal.remove()?

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

This lab's own `TransactionContextHolder.clear()` above runs in a `finally` block for exactly the same reason.

---

# 15. Why @Transactional Doesn't Work on Private Methods

Spring's transaction proxy for `EmployeeService` is a **CGLIB** proxy (subclassing), since the class doesn't implement an interface:

```text
EmployeeService (real class)
        ▲
        │ extends
EmployeeServiceProxy (CGLIB subclass)
```

A subclass can only override methods that are `public` or `protected` — never `private`, never `final`, never `static`.

```java
@Transactional
private void save() { ... } // proxy can't override this — runs with NO transaction, no error, no warning
```

That silent failure (no exception, just quietly-not-transactional behavior) is exactly what makes this a favorite interview trap.

> Every `@Transactional` method in this lab (`updateEmployee`, `updateSalaryWithFlush`, `updateSalaryOptimistic`, `updateSalaryWithAudit`, `AuditLogService.log`) is `public` — which is why they actually work.

---

# 16. Declarative vs Programmatic Transactions

Everywhere else in this guide, `@Transactional` is **declarative** — the framework wires up begin/commit/rollback around the whole method via the proxy.

Spring also supports **programmatic** transactions, calling `PlatformTransactionManager` (or its `TransactionTemplate` wrapper) directly, controlling exactly which lines run inside the transaction:

```java
private final TransactionTemplate transactionTemplate;

public Employee updateSalaryProgrammatic(Long id, Double salary) {
    return transactionTemplate.execute(status -> {
        Employee employee = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee Not Found"));
        employee.setSalary(salary);
        return employee;
    });
}
```

| Declarative (`@Transactional`) | Programmatic (`TransactionTemplate`) |
|---|---|
| Whole method is the transaction boundary | You choose exactly which lines are inside it |
| Needs a proxy (self-invocation trap applies) | No proxy involved — works even called via `this.` |
| Less code | More control, more boilerplate |

---

# 17. readOnly and timeout

```java
@Transactional(readOnly = true, timeout = 30)
public List<Employee> findAll() { ... }
```

- `readOnly = true` is a **hint**, not an enforced restriction — Hibernate uses it to skip dirty-checking snapshots, and some JDBC drivers use it to optimize the connection, but it won't actually stop a rogue `UPDATE` from running.
- `timeout = 30` aborts the transaction — rolling back whatever ran so far — if it hasn't completed within 30 seconds.

---

# 18. NESTED vs REQUIRES_NEW

Both start "inside" an existing transaction, but differently:

| `REQUIRES_NEW` | `NESTED` |
|---|---|
| Suspends the outer transaction, starts a **completely independent** one | Uses a JDBC **savepoint** inside the *same* physical transaction |
| Outer transaction's rollback has no effect on it | Rolling back the outer transaction also rolls back the nested savepoint |
| Two separate database transactions | One database transaction with a rollback point |

`AuditLogService.log(...)` in this lab uses `REQUIRES_NEW` specifically because the audit row must survive even if the outer transaction later rolls back — `NESTED` would roll the audit row back along with everything else, defeating the whole point of the demo in [Spring Data JPA §35](../../docs/03-Spring-JPA-README.md).

---

# 19. Transaction Synchronization Callbacks

`TransactionSynchronizationManager` (§12) doesn't just bind the connection — it also lets code register callbacks for transaction lifecycle events:

```java
TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
    @Override
    public void afterCommit() {
        // e.g. publish an event, send a notification — only if the commit actually succeeded
    }

    @Override
    public void afterCompletion(int status) {
        // runs whether committed or rolled back — cleanup that must always happen
    }
});
```

This is how "send an email only after the order is actually saved" is built correctly — inside the transaction, `afterCommit()` hasn't fired yet, so sending the email there risks notifying about a change that later rolls back.

---

## Startup vs Runtime — Quick Recap

**Startup** (runs once): `BeanPostProcessor → Reflection → TransactionAttributeSource → ProxyFactory → IOC Container`

**Runtime** (every request): `Proxy → TransactionInterceptor → PlatformTransactionManager → Business Logic → Commit / Rollback`

## ⭐ Interview Questions

- Why does `BeanPostProcessor` only run at startup, not per request?
- Why does Spring cache `@Transactional` metadata (`TransactionAttributeSource`) instead of re-reading annotations via reflection on every call?
- Why exactly one proxy per bean, not one per method?
- Why does self-invocation defeat `@Transactional` the same way it defeats `@Before`/`@After` logging?
- Where does the JDBC `Connection` actually come from, and why is `setAutoCommit(false)` required?
- How do `repository.save(...)` calls scattered across a transactional method all use the *same* connection without passing it explicitly?
- Why must `ThreadLocal.remove()` run at the end of every request on a pooled-thread server like Tomcat?
- Why does `@Transactional` on a `private` method fail silently instead of throwing an error?
- When would you reach for `TransactionTemplate` instead of `@Transactional`?
- What's the actual difference between `NESTED` and `REQUIRES_NEW`?

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
