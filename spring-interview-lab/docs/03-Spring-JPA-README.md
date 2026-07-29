# 📘 Spring Data JPA Interview Notes

<p align="center">
  <b>JPA · Hibernate · Spring Data JPA — Concepts, Relationships & Transactions</b>
</p>

---

## 📑 Table of Contents

1. [JPA](#1-what-is-jpa)
2. [Hibernate](#2-what-is-hibernate)
3. [Spring Data JPA](#3-what-is-spring-data-jpa)
4. [Entity](#4-entity)
5. [Repository](#5-repository)
6. [EntityManager](#6-entitymanager)
7. [Persistence Context](#7-persistence-context)
8. [Dirty Checking](#8-dirty-checking)
9. [Flush](#9-flush)
10. [Derived Query](#10-derived-query)
11. [JPQL](#11-jpql)
12. [Native Query](#12-native-query)
13. [Pagination](#13-pagination)
14. [Slice](#14-slice)
15. [Sorting](#15-sorting)
16. [Projection](#16-projection)
17. [Specification](#17-specification)
18. [QueryDSL](#18-querydsl)
19. [Dynamic SQL (EntityManager)](#19-dynamic-sql-entitymanager)
20. [Which Approach Should We Use?](#20-which-approach-should-we-use)
21. [JPA Relationships](#21-jpa-relationships)
22. [Owning Side](#22-owning-side)
23. [Inverse Side (mappedBy)](#23-inverse-side-mappedby)
24. [@JoinColumn](#24-joincolumn)
25. [FetchType](#25-fetchtype)
26. [Hibernate Proxy](#26-hibernate-proxy)
27. [LazyInitializationException](#27-lazyinitializationexception)
28. [N+1 Query Problem](#28-n1-query-problem)
29. [JOIN FETCH](#29-join-fetch)
30. [EntityGraph](#30-entitygraph)
31. [Cascade](#31-cascade)
32. [orphanRemoval](#32-orphanremoval)
33. [Optimistic Locking](#33-optimistic-locking)
34. [Transactions](#transaction)
35. [ACID Properties](#25-acid-properties)
36. [Transaction Propagation](#26-transaction-propagation)
37. [Transaction Isolation](#30-transaction-isolation)
38. [Rollback Rules](#35-rollback-rules)

---

## 1. What is JPA?

**Definition**
JPA (Java Persistence API) is a Java specification that defines how Java objects should be mapped to relational database tables.

> JPA is **not** a framework and contains **no implementation**. It's a contract.

It defines:
- ORM Rules
- Annotations
- Interfaces (`EntityManager`)

**Why JPA?**

| Without JPA | With JPA |
|---|---|
| Manual SQL | Automatic Mapping |
| Manual ResultSet Mapping | Automatic SQL Generation |
| More Boilerplate Code | Less Code |
| — | Better Maintainability |

**Architecture**

```text
Application
      │
Spring Data JPA
      │
     JPA
      │
 Hibernate
      │
   JDBC
      │
 Database
```

---

## 2. What is Hibernate?

**Definition**
Hibernate is the most popular implementation of JPA. It converts Java objects into SQL and SQL results back into Java objects.

**Responsibilities**
- SQL Generation
- ORM
- Persistence Context
- Dirty Checking
- Caching
- Lazy Loading
- Transaction Support

**Flow**

```text
Java Object → Hibernate → SQL → Database
```

---

## 3. What is Spring Data JPA?

**Definition**
Spring Data JPA is a Spring module built on top of JPA. It removes boilerplate code by providing Repository interfaces.

**Internal Working**

```text
Controller → Service → Repository → EntityManager → Hibernate → Database
```

**Advantages**
- No DAO implementation
- CRUD methods
- Pagination
- Sorting
- Dynamic Queries

---

## 4. Entity

**Definition**
An Entity is a Java class mapped to a database table. Each object represents one row in the table.

```text
Employee Object  ──▶  employee Table
```

**Example**

```java
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
}
```

**Entity Lifecycle**

```text
Transient ──persist()──▶ Managed ──detach()──▶ Detached ──remove()──▶ Removed
```

---

## 5. Repository

**Definition**
Repository is the data access layer. It communicates with the database through Hibernate.

**Repository Hierarchy**

```text
JpaRepository ▲ PagingAndSortingRepository ▲ CrudRepository ▲ Repository
```

**Common Features**
- Save / Update / Delete / Find
- Pagination
- Sorting

**Example**

```java
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    // save(), findById(), findAll(), deleteById() come for free
}
```

```java
@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public Employee create(Employee employee) {
        return employeeRepository.save(employee);
    }
}
```

---

## 6. EntityManager

**Definition**
EntityManager is the main JPA interface. It manages the lifecycle of entities and communicates with Hibernate.

**Responsibilities**
- Persist, Find, Merge, Remove, Flush
- Execute JPQL
- Execute Native SQL

**Internal Flow**

```text
Repository → EntityManager → Persistence Context → Hibernate → Database
```

**Example**

```java
@Repository
public class EmployeeDao {

    @PersistenceContext
    private EntityManager entityManager;

    public Employee find(Long id) {
        return entityManager.find(Employee.class, id);
    }

    @Transactional
    public void save(Employee employee) {
        entityManager.persist(employee);
    }

    @Transactional
    public Employee update(Employee employee) {
        return entityManager.merge(employee);
    }

    @Transactional
    public void delete(Employee employee) {
        entityManager.remove(employee);
    }
}
```

---

## 7. Persistence Context

**Definition**
Persistence Context is a memory area managed by EntityManager that stores Managed Entities. Also called the **First Level Cache**.

**Why?** Avoid repeated database calls.

**Internal Working**

```text
findById() → Persistence Context → Entity Found?
                                      ├── YES → Return Object
                                      └── NO  → Database
```

**Benefits**
- Less SQL
- Better Performance
- Same Object Returned
- Dirty Checking

**Example**

```java
@Transactional
public void demo(Long id) {
    Employee e1 = entityManager.find(Employee.class, id); // hits DB
    Employee e2 = entityManager.find(Employee.class, id); // returned from Persistence Context

    System.out.println(e1 == e2); // true — same managed instance
}
```

---

## 8. Dirty Checking

**Definition**
Hibernate automatically detects changes made to Managed Entities. At commit time it compares the current object with its snapshot — if changes exist, it generates `UPDATE` SQL automatically.

**Flow**

```text
Load Entity → Snapshot → Modify Object → Commit → Compare Snapshot → UPDATE SQL
```

**Why?** No need to call `save()` for managed entities.

**Example**

```java
@Transactional
public void updateEmail(Long id, String newEmail) {
    Employee employee = entityManager.find(Employee.class, id); // managed
    employee.setEmail(newEmail); // no explicit save() needed
    // UPDATE is issued automatically at commit time
}
```

---

## 9. Flush

**Definition**
Flush synchronizes the Persistence Context with the database. It sends SQL but does **not** commit the transaction.

```text
Persistence Context → flush() → SQL → Database
```

**Flush vs Commit**

| Flush | Commit |
|---|---|
| Sends SQL | Makes Permanent |
| Transaction Active | Transaction Ends |

**Example**

```java
@Transactional
public void demo() {
    Employee employee = entityManager.find(Employee.class, 1L);
    employee.setEmail("new@mail.com");

    entityManager.flush(); // SQL sent now, transaction still open
    // ... more work in the same transaction
}
```

---

## 10. Derived Query

**Definition**
Spring creates queries automatically from repository method names.

```text
Method Name → Spring Parser → JPQL → Hibernate → SQL
```

**Best Use:** Simple queries.
**Limitation:** Method names become too long.

**Example**

```java
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);

    List<Employee> findByNameAndEmail(String name, String email);

    List<Employee> findByNameContainingIgnoreCase(String keyword);

    boolean existsByEmail(String email);
}
```

---

## 11. JPQL

**Definition**
JPQL is an object-oriented query language. It works with **Entity Names / Fields**, not table names or columns.

```text
JPQL → Hibernate → SQL → Database
```

**Advantages**
- Database Independent
- Easy to Maintain
- Supports DTO Projection

**Example**

```java
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @Query("SELECT e FROM Employee e WHERE e.name = :name")
    List<Employee> searchByName(@Param("name") String name);

    @Query("SELECT e.email FROM Employee e WHERE e.id = :id")
    String findEmailById(@Param("id") Long id);
}
```

---

## 12. Native Query

**Definition**
Native Query uses actual database SQL — Hibernate executes it directly.

```text
Native SQL → EntityManager → Database
```

**Best Use:** Stored Procedures, CTEs, Window Functions, Vendor-specific SQL.

**Example**

```java
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @Query(value = "SELECT * FROM employees WHERE email = :email", nativeQuery = true)
    Employee findByEmailNative(@Param("email") String email);
}
```

---

## 13. Pagination

**Definition**
Pagination divides a large result into smaller pages.

```text
Client → Page Number → LIMIT → Database
```

**Page returns:** Records, Total Pages, Total Records
**Needs:** Two Queries — `LIMIT` + `COUNT`

**Example**

```java
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Page<Employee> findByName(String name, Pageable pageable);
}
```

```java
Pageable pageable = PageRequest.of(0, 10); // page 0, size 10
Page<Employee> page = employeeRepository.findByName("Neelu", pageable);

page.getContent();       // records in this page
page.getTotalPages();
page.getTotalElements();
```

---

## 14. Slice

**Definition**
Slice returns only the current page + `hasNext()`.

**Internal Working:** Fetches `Page Size + 1` records.
**Why Faster?** No `COUNT` query.

**Page vs Slice**

| Page | Slice |
|---|---|
| Two Queries | One Query |
| Total Pages | No Total Pages |
| Slower | Faster |

**Example**

```java
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Slice<Employee> findByName(String name, Pageable pageable);
}
```

```java
Slice<Employee> slice = employeeRepository.findByName("Neelu", PageRequest.of(0, 10));
slice.getContent();
slice.hasNext(); // no COUNT query executed
```

---

## 15. Sorting

**Definition**
Sorting arranges records in ascending or descending order, using **Entity Fields**, not database columns.

```text
Sort → JPQL → SQL → ORDER BY
```

**Example**

```java
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByName(String name, Sort sort);
}
```

```java
List<Employee> employees = employeeRepository.findByName(
        "Neelu", Sort.by(Sort.Direction.DESC, "id"));
```

---

## 16. Projection

**Definition**
Projection fetches only required fields instead of the entire entity.

**Why?** Better performance, less memory, less network traffic.

**Types:** Interface Projection · DTO Projection

**Comparison**

| Entity | Projection |
|---|---|
| All Columns | Selected Columns |
| Managed | Read-only |
| More Memory | Less Memory |

**Example — Interface Projection**

```java
public interface EmployeeNameOnly {
    String getName();
    String getEmail();
}
```

```java
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<EmployeeNameOnly> findByName(String name);
}
```

**Example — DTO Projection**

```java
public record EmployeeDto(String name, String email) {}
```

```java
@Query("SELECT new com.example.s3demo.dto.EmployeeDto(e.name, e.email) FROM Employee e")
List<EmployeeDto> findAllDtos();
```

---

## 17. Specification

**Definition**
Specification builds dynamic queries using the Criteria API. Conditions can be combined at runtime.

```text
Filters → Specification → Criteria API → SQL
```

**Best Use:** Optional search filters.
**Limitation:** Criteria API is verbose.

**Example**

```java
public interface EmployeeRepository
        extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {
}
```

```java
public class EmployeeSpecifications {

    public static Specification<Employee> hasName(String name) {
        return (root, query, cb) ->
                name == null ? null : cb.equal(root.get("name"), name);
    }

    public static Specification<Employee> hasEmail(String email) {
        return (root, query, cb) ->
                email == null ? null : cb.equal(root.get("email"), email);
    }
}
```

```java
Specification<Employee> spec = Specification
        .where(EmployeeSpecifications.hasName(name))
        .and(EmployeeSpecifications.hasEmail(email));

List<Employee> result = employeeRepository.findAll(spec);
```

---

## 18. QueryDSL

**Definition**
QueryDSL is a type-safe query framework. It generates `Q` classes for every entity.

```text
Entity → QEntity → BooleanBuilder → Hibernate → SQL
```

**Advantages**
- Type Safe
- IDE Auto Completion
- Readable & Easy to Maintain

**Best Use:** Enterprise search APIs.

**Example**

```java
QEmployee employee = QEmployee.employee; // generated Q-class

BooleanBuilder builder = new BooleanBuilder();
if (name != null) builder.and(employee.name.eq(name));
if (email != null) builder.and(employee.email.eq(email));

List<Employee> result = new JPAQueryFactory(entityManager)
        .selectFrom(employee)
        .where(builder)
        .fetch();
```

---

## 19. Dynamic SQL (EntityManager)

**Definition**
EntityManager allows SQL to be built dynamically at runtime — useful when the search column is chosen by the user.

```text
API (column=name, value=Neelu) → Validate Column → Build SQL → EntityManager → Database
```

> ⚠️ **Important:** Always validate dynamic column names. Never concatenate user input directly into SQL.

**Example**

```java
private static final Set<String> ALLOWED_COLUMNS = Set.of("name", "email", "id");

public List<Employee> searchByColumn(String column, String value) {
    if (!ALLOWED_COLUMNS.contains(column)) {
        throw new IllegalArgumentException("Invalid column: " + column);
    }

    String jpql = "SELECT e FROM Employee e WHERE e." + column + " = :value";

    return entityManager.createQuery(jpql, Employee.class)
            .setParameter("value", value)
            .getResultList();
}
```

---

## 20. Which Approach Should We Use?

| Requirement | Recommended Approach |
|---|---|
| CRUD | `JpaRepository` |
| Simple Search | Derived Query |
| Complex Fixed Query | JPQL |
| Database Specific SQL | Native Query |
| Pagination | `Page` |
| Infinite Scroll | `Slice` |
| Sorting | `Sort` |
| Few Columns | Projection |
| Optional Filters | Specification |
| Type-safe Dynamic Search | QueryDSL |
| Dynamic Column Search | EntityManager |

---

## 🔄 Spring Data JPA Complete Flow

```text
Client → Controller → Service → Repository → EntityManager
       → Persistence Context → Hibernate → JDBC → Database → Response
```

## 🗂️ Spring Data JPA Cheat Sheet

```text
Simple Query        → Derived Query
Complex Query        → JPQL
Database SQL         → Native Query
Pagination           → Page / Slice
Sorting              → Sort
Few Columns          → Projection
Dynamic Filters      → Specification
Type Safe Search     → QueryDSL
Dynamic SQL          → EntityManager
```

---

## 21. JPA Relationships

**Definition**
Relationships define how one entity is associated with another entity.

```text
JPA Relationships
├── OneToOne
├── OneToMany
├── ManyToOne
└── ManyToMany
```

| Relationship | Example |
|---|---|
| **OneToOne** | Employee → Locker |
| **OneToMany** | Department → Employees |
| **ManyToOne** | Employee → Department |
| **ManyToMany** | Student → Course |

**Example — ManyToMany**

```java
@Entity
public class Student {
    @Id @GeneratedValue
    private Long id;

    @ManyToMany
    @JoinTable(
        name = "student_course",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id"))
    private Set<Course> courses = new HashSet<>();
}
```

```java
@Entity
public class Course {
    @Id @GeneratedValue
    private Long id;

    @ManyToMany(mappedBy = "courses")
    private Set<Student> students = new HashSet<>();
}
```

---

## 22. Owning Side

**Definition**
The Owning Side is the entity that contains the Foreign Key and controls the relationship.

**Example:** `Locker` owns the relationship because it contains `employee_id`.

```java
@Entity
public class Locker {
    @Id @GeneratedValue
    private Long id;

    @OneToOne
    @JoinColumn(name = "employee_id") // FK lives here — Locker is the owner
    private Employee employee;
}
```

> ⚠️ Only the owning side updates the Foreign Key.

---

## 23. Inverse Side (mappedBy)

**Definition**
The Inverse Side is the non-owning side — it simply reflects the relationship already managed by the owning side. `Employee` does not contain a Foreign Key.

**Without `mappedBy`**

Hibernate thinks both `Employee` and `Locker` own the relationship → results in **two relationships** (extra join table or duplicate FK mapping).

**With `mappedBy`**

`Employee` holds a reference only; `Locker` owns the relationship → only **one** Foreign Key is maintained.

```java
@Entity
public class Employee {
    @Id @GeneratedValue
    private Long id;

    @OneToOne(mappedBy = "employee") // no FK here — just a reference
    private Locker locker;
}
```

---

## 24. @JoinColumn

**Definition**
`@JoinColumn` specifies the Foreign Key column.

**Example:** `Locker.employee_id` → references `Employee`. Hibernate creates `employee_id` inside the `locker` table.

```java
@ManyToOne
@JoinColumn(name = "department_id") // FK column name in the `employees` table
private Department department;
```

---

## 25. FetchType

**Definition**
FetchType decides when Hibernate loads associated entities.

| Type | Description | Advantages | Disadvantages |
|---|---|---|---|
| **LAZY** | Loads related data only when accessed | Better performance, less memory | — |
| **EAGER** | Loads related data immediately | Simple, no `LazyInitializationException` | More SQL, more memory |

**Default Fetch Types**

| Relationship | Default |
|---|---|
| OneToOne | EAGER |
| ManyToOne | EAGER |
| OneToMany | LAZY |
| ManyToMany | LAZY |

**Example**

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "department_id")
private Department department;

@OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
private List<Employee> employees = new ArrayList<>();
```

---

## 26. Hibernate Proxy

**Definition**
Hibernate creates a Proxy Object for LAZY associations. The proxy contains only the Primary Key, a session reference, and the logic to load the entity. Actual data loads only when accessed.

---

## 27. LazyInitializationException

**Definition**
Occurs when a LAZY entity is accessed after the Hibernate Session is closed.

```text
Session Open → Proxy → Session Closed → Access Proxy → LazyInitializationException
```

**Example**

```java
public Department getDepartment(Long id) {
    return departmentRepository.findById(id).orElseThrow(); // session closes when method returns
}

// later, outside a transaction/session:
department.getEmployees().size(); // throws LazyInitializationException
```

---

## 28. N+1 Query Problem

**Definition**
Hibernate executes 1 query for parent entities + N additional queries for child entities → **1 + N Queries**. Performance issue.

**Solution:** `JOIN FETCH`, `EntityGraph`, DTO Projection.

**Example — the problem**

```java
List<Department> departments = departmentRepository.findAll(); // 1 query

for (Department d : departments) {
    d.getEmployees().size(); // N additional queries — one per department
}
```

---

## 29. JOIN FETCH

**Definition**
Loads parent and child entities using a single SQL query.

**Benefits:** Eliminates N+1, better performance.

**Example**

```java
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    @Query("SELECT d FROM Department d JOIN FETCH d.employees")
    List<Department> findAllWithEmployees(); // single SQL query
}
```

---

## 30. EntityGraph

**Definition**
Overrides FetchType for a specific repository method.

**Benefits:** Cleaner than `JOIN FETCH`, query-specific fetch plan.

**Example**

```java
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    @EntityGraph(attributePaths = "employees")
    List<Department> findAll(); // overrides default LAZY fetch for this call
}
```

---

## 31. Cascade

**Definition**
Cascade propagates operations from Parent to Child.

| Cascade | Description |
|---|---|
| `PERSIST` | Save Child |
| `MERGE` | Update Child |
| `REMOVE` | Delete Child |
| `REFRESH` | Reload Child |
| `DETACH` | Detach Child |
| `ALL` | All Operations |

**Example**

```java
@Entity
public class Department {
    @Id @GeneratedValue
    private Long id;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
    private List<Employee> employees = new ArrayList<>();
}
```

```java
Department dept = new Department();
Employee emp = new Employee();
emp.setDepartment(dept);
dept.getEmployees().add(emp);

departmentRepository.save(dept); // emp is saved automatically via CascadeType.ALL
```

---

## 32. orphanRemoval

**Definition**
Deletes a child when it is removed from the parent's relationship.

**Example:** `Department → Employees` → remove Rahul from the collection → Rahul is deleted.

**Difference**

| `Cascade REMOVE` | `orphanRemoval` |
|---|---|
| Triggered when Parent is deleted | Triggered when child is removed from collection |
| Deletes all children | Deletes only the removed child |

**Example**

```java
@OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Employee> employees = new ArrayList<>();
```

```java
dept.getEmployees().remove(rahul); // Rahul is deleted from the DB on commit
```

---

## 33. Optimistic Locking

**Definition**
Uses `@Version` to prevent Lost Update problems.

```text
Read → Version = 1 → Update → Version Check → Success OR OptimisticLockException
```

**Example**

```java
@Entity
public class Employee {
    @Id @GeneratedValue
    private Long id;

    @Version
    private Long version;

    private String email;
}
```

```java
try {
    Employee employee = employeeRepository.findById(id).orElseThrow();
    employee.setEmail("updated@mail.com");
    employeeRepository.save(employee);
} catch (OptimisticLockException ex) {
    // another transaction updated this row first — retry or notify the user
}
```

---

## ⭐ Most Important JPA Interview Questions

- What is the difference between `@JoinColumn` and `mappedBy`?
- What is the Owning Side? What is the Inverse Side? Why do we use `mappedBy`?
- Difference between LAZY and EAGER.
- What is a Hibernate Proxy? Why does `LazyInitializationException` occur?
- What is the N+1 Query Problem?
- Difference between `JOIN FETCH` and `EntityGraph`.
- What is Cascade? Difference between `CascadeType.REMOVE` and `orphanRemoval`.
- Difference between `@OneToMany` and `@ManyToOne`.
- Difference between `@OneToOne` and `@ManyToOne`.
- What is Optimistic Locking?

---

# 💳 Transaction

**Definition**
A Transaction is a group of one or more database operations executed as a single unit of work — either **all** operations succeed, or **all** fail.

**Without Transaction:** Debit succeeds → Credit fails → Database inconsistent.
**With Transaction:**

```text
Begin → Debit → Credit → Success?
                            ├── YES → Commit
                            └── NO  → Rollback
```

---

## @Transactional

**Definition**
`@Transactional` tells Spring to execute all database operations inside a single transaction. Spring automatically begins, executes, commits on success, and rolls back on failure.

**Internal Flow**

```text
Controller → Service (@Transactional) → Spring Transaction Manager → Hibernate → Database
```

**Example**

```java
@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    @Transactional
    public Employee updateEmail(Long id, String newEmail) {
        Employee employee = employeeRepository.findById(id).orElseThrow();
        employee.setEmail(newEmail); // UPDATE committed automatically
        return employee;
    }
}
```

---

## Commit

**Definition**
Commit permanently saves all changes made during the transaction.

```text
Transaction → Execute SQL → Commit → Permanent Changes
```

## Rollback

**Definition**
Rollback cancels all changes made during the current transaction.

```text
Transaction → Execute SQL → Exception → Rollback → Previous State Restored
```

---

## ACID Properties

| Property | Meaning |
|---|---|
| **Atomicity** | All operations succeed or all fail |
| **Consistency** | Database always moves from one valid state to another |
| **Isolation** | Concurrent transactions do not interfere with each other |
| **Durability** | Committed data is permanently stored |

---

## Transaction Propagation

**Definition**
Propagation defines how a transactional method behaves when called by another transactional method.

```text
Propagation
├── REQUIRED
├── REQUIRES_NEW
├── SUPPORTS
├── MANDATORY
├── NOT_SUPPORTED
├── NEVER
└── NESTED
```

| Propagation | Behavior | Best Use |
|---|---|---|
| `REQUIRED` (default) | Join existing transaction, else create new | Order Processing, Payment, Inventory |
| `REQUIRES_NEW` | Always starts a new transaction; suspends any existing one | Audit Logging, Notifications, Payment History |
| `SUPPORTS` | Join if present, else execute without a transaction | — |

**Example**

```java
@Service
public class OrderService {

    private final AuditLogService auditLogService;

    @Transactional(propagation = Propagation.REQUIRED)
    public void placeOrder(Order order) {
        // ... save order, update inventory ...
        auditLogService.log("Order placed: " + order.getId());
    }
}

@Service
public class AuditLogService {

    // runs in its own transaction — commits even if placeOrder() later fails
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String message) {
        auditLogRepository.save(new AuditLog(message));
    }
}
```

---

## Transaction Isolation

**Definition**
Isolation controls how concurrent transactions interact.

**Concurrency Problems**

| Problem | Description |
|---|---|
| **Dirty Read** | Reading uncommitted data from another transaction |
| **Non-Repeatable Read** | Reading the same row twice, getting different values |
| **Phantom Read** | Running the same query twice, getting different row counts |

**Isolation Levels**

| Isolation Level | Dirty Read | Non-Repeatable Read | Phantom Read |
|---|---|---|---|
| `READ_UNCOMMITTED` | ✅ | ✅ | ✅ |
| `READ_COMMITTED` | ❌ | ✅ | ✅ |
| `REPEATABLE_READ` | ❌ | ❌ | ✅ |
| `SERIALIZABLE` | ❌ | ❌ | ❌ |

**Example**

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public BigDecimal getAccountBalance(Long accountId) {
    return accountRepository.findById(accountId).orElseThrow().getBalance();
}
```

**Default Isolation by Database**

| Database | Default |
|---|---|
| MySQL (InnoDB) | `REPEATABLE_READ` |
| Oracle | `READ_COMMITTED` |
| PostgreSQL | `READ_COMMITTED` |

---

## Rollback Rules

**Default Rule:** Spring automatically rolls back for `RuntimeException` and `Error`, but **not** for checked exceptions.

```text
Throwable
├── Error
└── Exception
      ├── RuntimeException   → Rollback (e.g. NullPointerException, ArithmeticException)
      └── Checked Exception  → Commit by default (e.g. IOException, SQLException)
```

**`rollbackFor`** — forces rollback even for checked exceptions.
**`noRollbackFor`** — prevents rollback for specified exceptions.

**Example**

```java
@Transactional(rollbackFor = IOException.class)
public void saveWithFile(Employee employee) throws IOException {
    employeeRepository.save(employee);
    fileService.writeAuditFile(employee); // rolls back save() if this throws IOException
}

@Transactional(noRollbackFor = IllegalStateException.class)
public void updateStatus(Employee employee) {
    employeeRepository.save(employee);
    if (employee.getStatus() == null) {
        throw new IllegalStateException("Status missing"); // commit still happens
    }
}
```

---

## 🧾 Spring Transaction Complete Flow

```text
Client → Controller → Service (@Transactional) → Spring Transaction Manager
       → Hibernate → JDBC → Database → Commit / Rollback → Response
```

## Transaction Lifecycle

```text
Begin → Execute SQL → Success?
                        ├── YES → Commit
                        └── NO  → Rollback
                                    → End
```

## 🗂️ Spring Transaction Cheat Sheet

**ACID**

```text
Atomicity    → All or Nothing
Consistency  → Valid Database State
Isolation    → No Concurrent Conflicts
Durability   → Permanent Data
```

**Propagation**

```text
REQUIRED      → Join Existing, Else Create
REQUIRES_NEW  → Always New Transaction
SUPPORTS      → Join If Present, Else Execute Normally
```

**Isolation**

```text
READ_UNCOMMITTED → Nothing Prevented
READ_COMMITTED   → Prevents Dirty Read
REPEATABLE_READ  → Prevents Dirty Read + Non-Repeatable Read
SERIALIZABLE     → Prevents Everything
```

**Rollback Rules**

```text
Runtime Exception  → Rollback
Checked Exception  → Commit
rollbackFor         → Force Rollback
noRollbackFor        → Force Commit
```

---

## ⭐ Most Asked Transaction Interview Questions

- What is a Transaction? What does `@Transactional` do?
- Explain ACID properties.
- Difference between Commit and Rollback.
- Explain Transaction Propagation. Difference between `REQUIRED` and `REQUIRES_NEW`.
- What is Transaction Isolation? Explain Dirty Read, Non-Repeatable Read, and Phantom Read.
- Compare all Isolation Levels. Default isolation level in MySQL and Oracle.
- When does Spring roll back a transaction?
- Difference between Checked and Runtime Exceptions in transactions.
- Explain `rollbackFor` and `noRollbackFor`.

---

<p align="center">📌 Revision notes — Spring Data JPA & Transactions</p>
