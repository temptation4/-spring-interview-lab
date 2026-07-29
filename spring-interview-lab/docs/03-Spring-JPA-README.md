# 📘 Spring Data JPA Interview Notes

A concise interview handbook covering **Spring Data JPA**, **Hibernate**, and **JPA** concepts with architecture diagrams, internal working, and interview-focused explanations.

> Every concept below links to its working implementation in this same lab (`com.interview.labs.jpa.*`) — run the app and hit the listed endpoint to see it in action.

---

# 📚 Table of Contents

1. What is JPA?
2. What is Hibernate?
3. What is Spring Data JPA?
4. Entity
5. Repository
6. EntityManager
7. Persistence Context
8. Dirty Checking
9. Flush
10. Derived Query
11. JPQL
12. Native Query
13. Pagination
14. Slice
15. Sorting
16. Projection
17. Specification
18. QueryDSL
19. Dynamic SQL (EntityManager)
20. Which Approach Should We Use?
21. JPA Relationships
22. Owning Side
23. Inverse Side (mappedBy)
24. @JoinColumn
25. FetchType
26. Hibernate Proxy
27. LazyInitializationException
28. N+1 Query Problem
29. JOIN FETCH
30. EntityGraph
31. Cascade
32. orphanRemoval
33. Optimistic Locking
34. Transactions & @Transactional
35. Transaction Propagation
36. Transaction Isolation
37. Rollback Rules

---

# 1. What is JPA?

## Definition

JPA (**Java Persistence API**) is a Java specification that defines a standard way to map Java objects to relational database tables.

JPA is **not a framework** and **does not provide an implementation**.

It defines:

- ORM Rules
- Annotations
- Interfaces (EntityManager)

Think of **JPA as a contract**.

---

## Why JPA?

### Without JPA

- Manual SQL
- Manual ResultSet Mapping
- Boilerplate JDBC Code
- Manual Transaction Management

### With JPA

- Automatic Object Mapping
- Automatic SQL Generation
- Less Boilerplate Code
- Better Maintainability
- Database Independence

---

## Architecture

```text
Application
      │
Spring Data JPA
      │
JPA Specification
      │
Hibernate
      │
JDBC
      │
Database
```

---

## Advantages

- Standard API
- Database Independent
- Supports ORM
- Reduces Boilerplate Code
- Easy Integration with Spring Boot

---

# 2. What is Hibernate?

## Definition

Hibernate is the most popular implementation of the JPA specification.

It converts Java Objects into SQL and converts SQL results back into Java Objects.

Hibernate performs all ORM-related operations.

---

## Responsibilities

- SQL Generation
- Object Relational Mapping (ORM)
- Persistence Context
- Dirty Checking
- Caching
- Lazy Loading
- Transaction Management
- Query Execution

---

## Hibernate Flow

```text
Java Object
      │
Hibernate
      │
Generated SQL
      │
JDBC
      │
Database
```

---

## Advantages

- Automatic SQL Generation
- Database Independent
- First Level Cache
- Lazy Loading
- Transaction Support
- Performance Optimization

---

# 3. What is Spring Data JPA?

## Definition

Spring Data JPA is a Spring module built on top of JPA.

It reduces boilerplate code by providing Repository interfaces for common database operations.

---

## Internal Working

```text
Client
      │
Controller
      │
Service
      │
Repository
      │
EntityManager
      │
Hibernate
      │
JDBC
      │
Database
```

---

## Advantages

- No DAO Implementation
- Built-in CRUD Methods
- Pagination Support
- Sorting Support
- Dynamic Queries
- Transaction Integration

---

# 4. Entity

## Definition

An Entity is a Java class mapped to a database table.

Each object of the entity represents **one row** in the database.

---

## Entity Mapping

```text
Employee Object
        │
        ▼
employee Table
```

---

## Code — `Employee.java`

```java
@Entity
@Table(name = "employee")
public class Employee {

    @Id
    private Long id;

    private String name;
    private Double salary;

    @Column(name = "department")
    private String departmentName;

    private String city;

    @Version
    private Integer version;
}
```

---

## Entity Lifecycle

```text
Transient
      │
persist()
      ▼
Managed
      │
detach()
      ▼
Detached
      │
remove()
      ▼
Removed
```

---

## Entity States

### Transient

- Object exists only in memory.
- Not managed by Hibernate.
- Not stored in the database.

### Managed

- Stored inside the Persistence Context.
- Hibernate tracks all changes.

### Detached

- No longer managed by Hibernate.
- Changes are not synchronized automatically.

### Removed

- Marked for deletion.
- Deleted from the database during transaction commit.

---

# 5. Repository

## Definition

Repository is the Data Access Layer.

It communicates with the database through Hibernate and provides CRUD operations.

---

## Repository Hierarchy

```text
Repository
      ▲
CrudRepository
      ▲
PagingAndSortingRepository
      ▲
JpaRepository
```

---

## Common Features

- Save
- Update
- Delete
- Find
- Pagination
- Sorting
- Batch Operations

---

## Advantages

- Less Boilerplate Code
- Easy CRUD Operations
- Easy Pagination
- Easy Sorting
- Better Maintainability

---

## Code — `EmployeeRepository.java`

```java
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    // save(), findById(), findAll(), deleteById() — all inherited, zero implementation
}
```

---
# 6. EntityManager

## Definition

EntityManager is the core JPA interface responsible for managing the lifecycle of entities.

It acts as a bridge between the application and Hibernate.

---

## Responsibilities

- Persist Entity
- Find Entity
- Merge Entity
- Remove Entity
- Flush Changes
- Execute JPQL
- Execute Native SQL
- Manage Persistence Context

---

## Internal Working

```text
Repository
      │
EntityManager
      │
Persistence Context
      │
Hibernate
      │
JDBC
      │
Database
```

---

## Advantages

- Manages Entity Lifecycle
- Maintains Persistence Context
- Executes JPQL and Native SQL
- Handles Transactions
- Performs Dirty Checking

---

## Code — `EmployeeService.java` (dynamic SQL via EntityManager)

```java
private final EntityManager entityManager;

public List<Employee> search(String column, String value) {

    if (!ALLOWED_COLUMNS.contains(column)) {
        throw new IllegalArgumentException("Invalid Column Name");
    }

    String sql = "SELECT * FROM employee WHERE " + column + " = :value";

    Query query = entityManager.createNativeQuery(sql, Employee.class);
    query.setParameter("value", value);

    return query.getResultList();
}
```

**Endpoint:** `GET /employee/search?column=city&value=Pune`

---

# 7. Persistence Context

## Definition

Persistence Context is a memory area managed by the EntityManager.

It stores all **Managed Entities** during a transaction.

It is also known as the **First Level Cache**.

---

## Why do we need Persistence Context?

Without Persistence Context

- Every `findById()` executes SQL.
- Same object may be loaded multiple times.
- More database calls.

With Persistence Context

- Entity is loaded only once.
- Subsequent requests return the cached object.
- Improves application performance.

---

## Internal Working

```text
findById()
      │
Persistence Context
      │
Entity Found?
 YES        NO
 │          │
 ▼          ▼
Return    Database
Object       │
             ▼
      Store in Cache
             │
             ▼
       Return Object
```

---

## Benefits

- First Level Cache
- Less SQL Execution
- Better Performance
- Same Object Returned
- Supports Dirty Checking

---

## Code

```java
@Transactional
public void demo(Long id) {
    Employee e1 = repository.findById(id).orElseThrow(); // hits the DB
    Employee e2 = repository.findById(id).orElseThrow(); // returned from the Persistence Context

    System.out.println(e1 == e2); // true — same managed instance, no second SELECT
}
```

---

# 8. Dirty Checking

## Definition

Dirty Checking is a Hibernate feature that automatically detects changes made to **Managed Entities**.

When the transaction commits, Hibernate compares the current entity with its original snapshot.

If changes are found, Hibernate generates an **UPDATE** statement automatically.

---

## Why Dirty Checking?

Without Dirty Checking

- Every update requires an explicit save operation.

With Dirty Checking

- Simply modify the managed entity.
- Hibernate updates the database automatically.

---

## Internal Working

```text
Load Entity
      │
Create Snapshot
      │
Modify Entity
      │
Transaction Commit
      │
Compare Snapshot
      │
Generate UPDATE SQL
      │
Database
```

---

## Benefits

- Automatic Updates
- Less Boilerplate Code
- Better Performance
- Easy Transaction Management

---

## Code — `EmployeeService.java`

```java
@Transactional
public Employee updateEmployee(Employee request) {

    Employee employee = repository.findById(request.getId())
            .orElseThrow(() -> new RuntimeException("Employee Not Found"));

    employee.setSalary(request.getSalary()); // no explicit save() — Hibernate detects the change and issues UPDATE at commit

    return employee;
}
```

**Endpoint:** `PUT /employee`

---

# 9. Flush

## Definition

Flush synchronizes the Persistence Context with the database.

It sends SQL statements to the database but **does not commit** the transaction.

---

## Internal Working

```text
Persistence Context
      │
flush()
      │
SQL Generated
      │
Database
```

---

## Flush vs Commit

| Flush | Commit |
|--------|---------|
| Sends SQL | Makes Changes Permanent |
| Transaction Still Active | Transaction Ends |
| Can Rollback | Cannot Rollback After Commit |

---

## Why Flush?

Flush ensures that the database stays synchronized with the Persistence Context before committing the transaction.

---

## Code — `EmployeeService.java`

```java
@Transactional
public Employee updateSalaryWithFlush(Long id, Double salary) {

    Employee employee = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Employee Not Found"));

    employee.setSalary(salary);
    entityManager.flush(); // UPDATE sent now, transaction still open

    return employee;
}
```

**Endpoint:** `PUT /employee/{id}/salary/flush?salary=90000`

---

# Spring Data JPA Internal Flow

```text
Client
      │
Controller
      │
Service
      │
Repository
      │
EntityManager
      │
Persistence Context
      │
Hibernate
      │
JDBC
      │
Database
      │
Response
```

---

## Interview Tips

### What is EntityManager?

The core JPA interface responsible for managing entity lifecycle and interacting with the Persistence Context.

---

### What is Persistence Context?

A memory area managed by EntityManager that stores managed entities and acts as the First Level Cache.

---

### What is Dirty Checking?

Hibernate automatically detects changes made to managed entities and updates the database during transaction commit.

---

### What is Flush?

Flush synchronizes the Persistence Context with the database by executing SQL statements without committing the transaction.

---

## Quick Revision

```text
EntityManager
      │
Persistence Context
      │
Managed Entity
      │
Dirty Checking
      │
Flush
      │
Hibernate
      │
Database
```

---
# 10. Derived Query

## Definition

Derived Query Methods are queries automatically generated by Spring Data JPA based on the repository method name.

Spring analyzes the method name and creates the appropriate JPQL query internally.

---

## Why Derived Query?

Without Derived Query

- Write JPQL
- Write SQL
- Implement Repository Logic

With Derived Query

- Only define the repository method
- Spring generates the query automatically

---

## Internal Working

```text
Repository Method
      │
Spring Parser
      │
Generate JPQL
      │
Hibernate
      │
Generate SQL
      │
JDBC
      │
Database
```

---

## Advantages

- No SQL Required
- Easy to Read
- Less Boilerplate Code
- Faster Development

---

## Limitations

As the number of conditions increases, repository method names become very long and difficult to maintain.

---

## Best Use

- Simple Search
- Single Column Search
- Small Number of Conditions

---

## Code — `EmployeeRepository.java`

```java
List<Employee> findByDepartmentName(String departmentName);

List<Employee> findByCityAndSalaryGreaterThan(String city, Double salary);

boolean existsByName(String name);
```

**Endpoints:**
`GET /employee/derived/department?departmentName=Engineering`
`GET /employee/derived/city-salary?city=Pune&salary=50000`

---

# 11. JPQL

## Definition

JPQL (Java Persistence Query Language) is an object-oriented query language provided by JPA.

JPQL works with:

- Entity Names
- Entity Fields

JPQL does **not** use:

- Database Tables
- Database Columns

Hibernate converts JPQL into SQL before executing it.

---

## Why JPQL?

Derived Query Methods are suitable for simple queries.

For complex business logic, JPQL provides better readability and flexibility.

---

## Internal Working

```text
JPQL
      │
EntityManager
      │
Hibernate Parser
      │
Generate SQL
      │
JDBC
      │
Database
```

---

## Advantages

- Database Independent
- Supports Joins
- Supports DTO Projection
- Easy to Maintain
- Uses Entity Model

---

## Limitations

- Cannot use database-specific features.
- Vendor-specific functions may require Native Query.

---

## Best Use

- Complex Search
- Multiple Conditions
- Joins
- Business Reports

---

## Code — `EmployeeRepository.java`

```java
@Query("""
        SELECT e
        FROM Employee e
        WHERE e.salary > :salary
        """)
List<Employee> findEmployeesBySalary(@Param("salary") Double salary);
```

**Endpoint:** `GET /employee/salary/jpql/{salary}`

---

# 12. Native Query

## Definition

Native Query executes database-specific SQL directly.

Unlike JPQL, Hibernate does not convert the query.

The SQL is sent directly to the database.

---

## Internal Working

```text
Native SQL
      │
EntityManager
      │
Hibernate
      │
JDBC
      │
Database
```

---

## Advantages

- Full SQL Support
- Database-specific Features
- Window Functions
- Stored Procedures
- CTE Support

---

## Limitations

- Database Dependent
- Less Portable
- Harder to Maintain

---

## Best Use

- Stored Procedures
- Complex SQL
- Performance Tuning
- Vendor-specific Queries

---

## Code — `EmployeeRepository.java`

```java
@Query(value = """
        SELECT *
        FROM employee
        WHERE salary > :salary
        """, nativeQuery = true)
List<Employee> findEmployeesNative(@Param("salary") Double salary);
```

**Endpoint:** `GET /employee/salary/{salary}`

---

# JPQL vs Native Query

| JPQL | Native Query |
|------|--------------|
| Entity Name | Table Name |
| Entity Field | Database Column |
| Database Independent | Database Specific |
| Hibernate Converts to SQL | Executes SQL Directly |
| Easy to Maintain | More Powerful |

---

# 13. Pagination

## Definition

Pagination divides a large dataset into smaller pages.

Instead of loading all records, only the required page is retrieved.

---

## Why Pagination?

Without Pagination

- Loads all records
- High Memory Usage
- Slow Response
- Poor Performance

With Pagination

- Loads only required records
- Faster Response
- Better User Experience

---

## Internal Working

```text
Client
      │
Page Number
      │
Page Size
      │
Repository
      │
LIMIT
      │
Database
```

---

## Page

Page returns:

- Records
- Total Pages
- Total Records
- Current Page
- hasNext()
- hasPrevious()

---

## SQL Generated

```text
Query 1
LIMIT

+

Query 2
COUNT
```

---

## Advantages

- Total Pages Available
- Total Records Available
- Suitable for Reports
- Suitable for Admin Screens

---

## Code — `EmployeeService.java` / `EmployeeController.java`

```java
public Page<Employee> getEmployees(int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    return repository.findAll(pageable);
}
```

**Endpoint:** `GET /employee/page?page=0&size=10`

---

# 14. Slice

## Definition

Slice returns only:

- Current Page
- hasNext()

It does **not** calculate the total number of pages.

---

## Internal Working

Instead of executing COUNT,

Spring fetches

```text
Page Size + 1
```

records.

If one extra record exists,

```text
hasNext = true
```

Otherwise,

```text
hasNext = false
```

---

## Internal Flow

```text
Client
      │
Page Request
      │
LIMIT (Page Size + 1)
      │
Database
      │
Check Extra Record
      │
hasNext()
```

---

## Advantages

- Faster than Page
- No COUNT Query
- Better Performance
- Ideal for Large Datasets

---

## Code — `EmployeeRepository.java` / `EmployeeService.java`

```java
// findAllBy + Slice return type -> Spring fetches pageSize + 1 rows, no COUNT query.
// (findAll(pageable) would return a Page under the hood and always COUNT.)
Slice<Employee> findAllBy(Pageable pageable);
```

```java
public Slice<Employee> getEmployees1(int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    return repository.findAllBy(pageable);
}
```

**Endpoint:** `GET /employee/slice?page=0&size=10`

---

# Page vs Slice

| Page | Slice |
|------|--------|
| LIMIT + COUNT | LIMIT Only |
| Total Pages | No Total Pages |
| Total Records | No Total Records |
| Slower | Faster |
| Reports | Infinite Scroll |

---

# 15. Sorting

## Definition

Sorting arranges records in ascending or descending order.

Sorting always uses **Entity Fields**, not database column names.

---

## Internal Working

```text
Sort
      │
JPQL
      │
Hibernate
      │
ORDER BY
      │
Database
```

---

## Sorting Types

- Ascending
- Descending
- Multiple Columns

---

## Advantages

- Ordered Results
- Easy Pagination
- Better User Experience

---

## Best Use

- Employee List
- Product List
- Reports
- Search Results

---

## Code — `EmployeeService.java`

```java
public List<Employee> getEmployees() {
    return repository.findAll(Sort.by("salary").descending());
}
```

**Endpoint:** `GET /employee/sort`

---

# Query Flow Summary

```text
Simple Search
      │
Derived Query

----------------------

Complex Query
      │
JPQL

----------------------

Database Specific SQL
      │
Native Query

----------------------

Large Dataset
      │
Pagination

----------------------

Infinite Scroll
      │
Slice

----------------------

Ordered Results
      │
Sorting
```

---

# Interview Tips

### What is a Derived Query?

Spring automatically creates the query from the repository method name.

---

### Why does JPQL use Entity Names?

JPQL is object-oriented and works on Java entities rather than database tables.

---

### When should we use Native Query?

When database-specific SQL features are required.

---

### Why is Slice faster than Page?

Slice executes only one SQL query because it does not perform COUNT.

---

### Why does Page execute two SQL queries?

One query retrieves the records, and another COUNT query calculates the total number of records.

---

### Does Sorting use table columns?

No.

Sorting always uses **Entity Field Names**.

---

# Quick Revision

```text
Simple Query
      │
Derived Query

Complex Query
      │
JPQL

Database SQL
      │
Native Query

Large Dataset
      │
Page

Infinite Scroll
      │
Slice

Sorting
      │
ORDER BY
```

---
# 16. Projection

## Definition

Projection is a technique used to fetch **only the required columns** from the database instead of loading the complete entity.

Instead of retrieving all entity fields, Projection returns only the fields needed by the application.

---

## Why Projection?

Without Projection

- Fetches all columns
- Higher Memory Usage
- More Network Traffic
- Slower Queries

With Projection

- Fetches only required columns
- Less Memory Usage
- Faster Queries
- Better Performance

---

## Types of Projection

```text
Projection
      │
      ├── Interface Projection
      └── DTO Projection
```

---

## Internal Working

```text
Repository
      │
Projection
      │
Hibernate
      │
SELECT Required Columns
      │
Database
```

---

## Entity vs Projection

| Entity | Projection |
|---------|------------|
| Fetches All Columns | Fetches Required Columns |
| Managed by Hibernate | Read Only |
| More Memory | Less Memory |
| Slower | Faster |

---

## Best Use

- Dashboard
- Reports
- REST APIs
- Read-only Operations

---

## Code — Interface Projection

```java
public interface EmployeeView {
    String getName();
    Double getSalary();
}
```

```java
@Query("""
        SELECT
        e.name as name,
        e.salary as salary
        FROM Employee e
        """)
List<EmployeeView> getEmployeeView();
```

**Endpoint:** `GET /employee/projection`

## Code — DTO Projection

```java
@Query("""
        SELECT new com.interview.labs.jpa.dto.EmployeeDto(
                e.name,
                e.salary)
        FROM Employee e
        """)
List<EmployeeDto> findEmployee();
```

**Endpoint:** `GET /employee/projectionDto`

## Code — Interface Projection via a real Native Query

```java
@Query(value = """
        SELECT e.name AS name, e.salary AS salary
        FROM employee e
        """, nativeQuery = true)
List<EmployeeView> getEmployeeViewNative();
```

**Endpoint:** `GET /employee/projectionNative`

> **Note:** this used to be a copy-paste of the JPQL DTO query above (mislabeled as "native" while actually running JPQL). Fixed to be a genuine `nativeQuery = true` query — JPQL's `SELECT NEW ...` constructor expression only works in JPQL, not native SQL, so native + projection has to go through an interface projection instead.

---

# 17. Specification

## Definition

Specification is a Spring Data JPA feature used to build **dynamic queries** using the Criteria API.

Each search condition is created separately and combined at runtime.

---

## Why Specification?

Suppose an Employee Search screen contains:

- Name
- Department
- City
- Salary

Users may search using any combination of filters.

Instead of creating many repository methods, Specification builds the query dynamically.

---

## Internal Working

```text
Search Filters
      │
Specification
      │
Criteria API
      │
Predicate
      │
Hibernate
      │
SQL
      │
Database
```

---

## Advantages

- Dynamic Queries
- Reusable Conditions
- Easy Combination of Filters
- Spring Standard

---

## Limitations

- Criteria API syntax is verbose.
- Less readable than QueryDSL.

---

## Best Use

- Employee Search
- Product Search
- Admin Filter Screen
- Report Filters

---

## Code — `EmployeeSpecification.java`

```java
public class EmployeeSpecification {

    public static Specification<Employee> hasSalaryGreaterThan(Double salary) {
        return (root, query, cb) ->
                cb.greaterThan(root.get("salary"), salary);
    }
}
```

```java
public List<Employee> findEmployee(Double salary) {
    return repository.findAll(EmployeeSpecification.hasSalaryGreaterThan(salary));
}
```

**Endpoint:** `GET /employee/specification?salary=50000`

---

# 18. QueryDSL

## Definition

QueryDSL is a **type-safe query framework** for building dynamic queries.

It generates **Q Classes** for every entity and provides compile-time checking.

---

## Why QueryDSL?

Instead of using String-based field names,

QueryDSL provides strongly typed entity fields with IDE auto-completion.

---

## Internal Working

```text
Entity
      │
QEntity
      │
BooleanBuilder
      │
Predicate
      │
Hibernate
      │
SQL
      │
Database
```

---

## Advantages

- Type Safe
- Compile-time Validation
- IDE Auto-completion
- Easy to Read
- Easy Maintenance

---

## Limitations

- Requires Annotation Processing
- Additional Maven Dependency
- Generates Q Classes

---

## Best Use

- Enterprise Search APIs
- Complex Dynamic Queries
- Large Enterprise Applications

---

## Code — `EmployeeService.java`

```java
public Iterable<Employee> search(Double salary, String name) {

    QEmployee employee = QEmployee.employee; // generated by the QueryDSL annotation processor

    BooleanBuilder builder = new BooleanBuilder();

    if (salary != null) builder.and(employee.salary.gt(salary));
    if (name != null) builder.and(employee.name.eq(name));

    return repository.findAll(builder);
}
```

**Endpoint:** `GET /employee/querydsl?salary=50000&name=Neelu`

---

# Specification vs QueryDSL

| Specification | QueryDSL |
|--------------|----------|
| Criteria API | Type-safe API |
| Uses String Field Names | Uses Generated Q Classes |
| More Boilerplate | Cleaner Syntax |
| Spring Standard | Additional Dependency |
| Less Readable | More Readable |

---

# 19. Dynamic SQL (EntityManager)

## Definition

EntityManager allows SQL queries to be built dynamically at runtime.

This approach is useful when users select the column to search.

---

## Example Scenario

Search Screen

```text
Search By

▼ Name

▼ Department

▼ City

▼ Salary
```

Instead of creating multiple APIs,

one API builds SQL dynamically.

---

## Internal Working

```text
API Request
      │
Column Validation
      │
Build SQL
      │
EntityManager
      │
Hibernate
      │
Database
```

---

## Important

Always validate dynamic column names before building SQL.

Only parameterize values.

Never concatenate untrusted user input into SQL.

---

## Advantages

- Highly Flexible
- Supports Dynamic Columns
- Generic Search API

---

## Limitations

- Manual SQL Construction
- Harder to Maintain
- Greater Risk if Column Names Are Not Validated

---

## Best Use

- Generic Search Screens
- Dynamic Reporting
- Admin Search Tools

---

## Code — `EmployeeService.java`

```java
private static final Set<String> ALLOWED_COLUMNS =
        Set.of("name", "salary", "department", "city");

public List<Employee> search(String column, String value) {

    if (!ALLOWED_COLUMNS.contains(column)) {
        throw new IllegalArgumentException("Invalid Column Name");
    }

    String sql = "SELECT * FROM employee WHERE " + column + " = :value";

    Query query = entityManager.createNativeQuery(sql, Employee.class);

    switch (column) {
        case "id" -> query.setParameter("value", Long.parseLong(value));
        case "salary" -> query.setParameter("value", Double.parseDouble(value));
        case "version" -> query.setParameter("value", Integer.parseInt(value));
        case "name", "department", "city" -> query.setParameter("value", value);
        default -> throw new IllegalArgumentException("Unsupported Column");
    }

    return query.getResultList();
}
```

**Endpoint:** `GET /employee/search?column=city&value=Pune`

> The column name is validated against `ALLOWED_COLUMNS` before it's concatenated into SQL — only the **value** is bound as a parameter. That's the difference between a safe dynamic-column search and a SQL injection vulnerability.

---

# 20. Which Approach Should We Use?

| Requirement | Recommended Approach |
|--------------|----------------------|
| CRUD Operations | JpaRepository |
| Simple Search | Derived Query |
| Complex Business Query | JPQL |
| Database-specific SQL | Native Query |
| Pagination | Page |
| Infinite Scroll | Slice |
| Sorting | Sort |
| Fetch Few Columns | Projection |
| Optional Search Filters | Specification |
| Enterprise Dynamic Search | QueryDSL |
| Dynamic Column Search | EntityManager |

---

# Spring Data JPA Complete Flow

```text
Client
      │
Controller
      │
Service
      │
Repository
      │
EntityManager
      │
Persistence Context
      │
Hibernate
      │
JDBC
      │
Database
      │
Response
```

---

# Spring Data JPA Complete Cheat Sheet

```text
Need CRUD
      │
JpaRepository

------------------------

Need Simple Search
      │
Derived Query

------------------------

Need Complex Business Query
      │
JPQL

------------------------

Need Database-specific SQL
      │
Native Query

------------------------

Need Pagination
      │
Page

------------------------

Need Infinite Scroll
      │
Slice

------------------------

Need Sorting
      │
Sort

------------------------

Need Few Columns
      │
Projection

------------------------

Need Optional Filters
      │
Specification

------------------------

Need Enterprise Search
      │
QueryDSL

------------------------

Need Dynamic Column Search
      │
EntityManager
```

---

# 21. JPA Relationships

## Definition

Relationships define how one entity is associated with another.

```text
JPA Relationships
      │
      ├── OneToOne
      ├── OneToMany
      ├── ManyToOne
      └── ManyToMany
```

| Relationship | In this lab |
|---|---|
| OneToOne | `Employee` ↔ `Locker` |
| OneToMany / ManyToOne | `Department` ↔ `Employee` |
| ManyToMany | `Employee` ↔ `Project` |

---

# 22. Owning Side

## Definition

The Owning Side holds the Foreign Key and is the side Hibernate actually persists.

## Code — `Locker.java` (owning side of Employee ↔ Locker)

```java
@Entity
@Table(name = "locker")
public class Locker {

    @Id
    private Long id;

    private String lockerNumber;

    @OneToOne
    @JoinColumn(name = "employee_id") // FK lives here — Locker owns the relationship
    private Employee employee;
}
```

> Only the owning side's field assignment is what Hibernate writes to the Foreign Key column.

---

# 23. Inverse Side (mappedBy)

## Definition

The Inverse Side reflects a relationship it doesn't own — it holds no Foreign Key.

## Code — `Employee.java` (inverse side)

```java
@OneToOne(mappedBy = "employee")
@JsonIgnore // breaks the Employee <-> Locker JSON cycle — see note below
private Locker locker;
```

**Without `mappedBy`**, Hibernate would think both `Employee` and `Locker` own the relationship, and would try to maintain two independent Foreign Key mappings (or an unwanted join table).

> **Fixed while reviewing this code:** `Employee.locker` and `Locker.employee` point at each other. Serializing an `Employee` straight from a controller (several endpoints return `List<Employee>` directly) would recurse: `employee → locker → employee → locker → …` until `StackOverflowError`. Added `@JsonIgnore` on `Employee.locker` to break the cycle — the dedicated `/employee/locker` endpoint already returns a `LockerResponse` DTO built from direct field access, so it's unaffected.

---

# 24. @JoinColumn

## Definition

`@JoinColumn` names the Foreign Key column.

## Code — `Employee.java` (ManyToOne to Department)

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "department_id") // FK column created in the `employee` table
private Department department;
```

---

# 25. FetchType

## Definition

FetchType controls when Hibernate loads an association.

| Type | Behavior |
|---|---|
| **LAZY** | Loaded only when accessed — proxy until then |
| **EAGER** | Loaded immediately, in the same query or a join |

## Default Fetch Types

| Relationship | Default |
|---|---|
| OneToOne | EAGER |
| ManyToOne | EAGER |
| OneToMany | LAZY |
| ManyToMany | LAZY |

## Code — `Employee.java` / `Department.java`

```java
@ManyToOne(fetch = FetchType.LAZY)          // overridden from the EAGER default
@JoinColumn(name = "department_id")
private Department department;

@OneToMany(mappedBy = "department", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
private List<Employee> employees = new ArrayList<>();
```

> Both associations here are explicitly LAZY. Returning them as JSON still works because Spring Boot's `spring.jpa.open-in-view` defaults to `true` — the Hibernate session stays open through response serialization, so the proxies can initialize on demand. Disable OSIV and these same endpoints would throw `LazyInitializationException` unless the data is fetched inside the `@Transactional` service method first (see `JOIN FETCH` / `EntityGraph` below).

---

# 26. Hibernate Proxy

## Definition

For a LAZY association, Hibernate injects a proxy object holding only the Primary Key and a session reference — the real data loads on first access (e.g., `department.getEmployees().size()`).

---

# 27. LazyInitializationException

## Definition

Thrown when a LAZY association is accessed after its Hibernate session has closed.

## How this lab avoids it

`spring.jpa.open-in-view=true` (Spring Boot default, not overridden in `application.properties`) keeps the session open for the whole request, so `Department.employees` and `Employee.department`/`projects` can be serialized straight from a controller without a `LazyInitializationException`. It would resurface if OSIV were disabled and a LAZY field were touched outside the `@Transactional` boundary — the fix is always the same: fetch what you need (`JOIN FETCH` / `@EntityGraph`) inside the transactional method.

---

# 28. N+1 Query Problem

## Definition

Fetching a list of parents (1 query) and then lazily touching each parent's children (N more queries) → **1 + N** total queries.

## Code — `DepartmentService.java` (the problem, made observable on purpose)

```java
public List<Department> getDepartmentsNPlusOne() {
    List<Department> departments = departmentRepository.findAll();      // 1 query
    departments.forEach(department -> department.getEmployees().size()); // N queries
    return departments;
}
```

**Endpoint:** `GET /department/n-plus-one` — watch the console (`spring.jpa.show-sql=true` is already on) to see one `SELECT` per department.

---

# 29. JOIN FETCH

## Definition

Loads the parent and its children in a single SQL query using a JPQL join.

## Code — `DepartmentRepository.java`

```java
@Query("SELECT d FROM Department d JOIN FETCH d.employees")
List<Department> findAllWithEmployeesJoinFetch();
```

**Endpoint:** `GET /department/join-fetch` — one query total, no matter how many departments exist.

---

# 30. EntityGraph

## Definition

Declares a per-query fetch plan without changing the entity's default FetchType.

## Code — `DepartmentRepository.java`

```java
@EntityGraph(attributePaths = "employees")
@Query("SELECT d FROM Department d")
List<Department> findAllWithEmployeesEntityGraph();
```

**Endpoint:** `GET /department/entity-graph`

---

# 31. Cascade

## Definition

Propagates persistence operations from a parent entity to its children.

## Code — `Department.java`

```java
@OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
private List<Employee> employees = new ArrayList<>();
```

`CascadeType.ALL` means saving/deleting a `Department` cascades to its `Employee` rows automatically — no need to save each `Employee` separately.

---

# 32. orphanRemoval

## Definition

Deletes a child the moment it's removed from the parent's collection — distinct from `Cascade REMOVE`, which only fires when the parent itself is deleted.

## Code — `DepartmentService.java`

```java
@Transactional
public void removeEmployeeFromDepartment(Long departmentId, Long employeeId) {
    Department department = departmentRepository.findById(departmentId)
            .orElseThrow(() -> new RuntimeException("Department Not Found"));

    department.getEmployees().removeIf(employee -> employee.getId().equals(employeeId));
    // orphanRemoval = true on Department.employees -> this Employee row is deleted, not just unlinked
}
```

**Endpoint:** `DELETE /department/{departmentId}/employee/{employeeId}`

---

# 33. Optimistic Locking

## Definition

Uses `@Version` to detect a lost update: Hibernate's `UPDATE ... WHERE id = ? AND version = ?` matches zero rows if someone else updated the row first, and throws `OptimisticLockException`.

## Code — `Employee.java`

```java
@Version
private Integer version;
```

## Code — `EmployeeService.java`

```java
@Transactional
public Employee updateSalaryOptimistic(Long id, Double salary, Integer expectedVersion) {

    Employee employee = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Employee Not Found"));

    entityManager.detach(employee);
    employee.setSalary(salary);
    employee.setVersion(expectedVersion); // the version the caller last read

    try {
        return entityManager.merge(employee); // fails if the DB's current version != expectedVersion
    } catch (OptimisticLockException ex) {
        throw new IllegalStateException(
                "Employee was modified by another transaction. Please reload and retry.", ex);
    }
}
```

**Endpoint:** `PUT /employee/{id}/salary/optimistic?salary=90000&version=1`
Read the employee first to get its current `version`; pass a **stale** version to see `OptimisticLockException` fire, or the current one to see it succeed.

---

## ⭐ Most Important Relationship Interview Questions

- `@JoinColumn` vs `mappedBy` — which side owns the Foreign Key?
- Why does removing `mappedBy` create a duplicate/extra relationship?
- LAZY vs EAGER — and the default for each relationship type.
- What is a Hibernate proxy, and when does `LazyInitializationException` fire?
- What is the N+1 problem, and how do `JOIN FETCH` / `@EntityGraph` fix it?
- `CascadeType.REMOVE` vs `orphanRemoval` — what's the actual difference?
- How does `@Version` prevent a lost update?

---

# 34. Transactions & @Transactional

## Definition

A Transaction groups one or more database operations into a single unit of work — either all succeed, or all are rolled back. `@Transactional` tells Spring to manage that unit of work automatically: begin, commit on success, roll back on failure.

## Code — `EmployeeService.java`

```java
@Transactional
public Employee updateEmployee(Employee request) {
    Employee employee = repository.findById(request.getId())
            .orElseThrow(() -> new RuntimeException("Employee Not Found"));

    employee.setSalary(request.getSalary()); // committed automatically when the method returns
    return employee;
}
```

---

# 35. Transaction Propagation

## Definition

Propagation decides how a `@Transactional` method behaves when called from inside another transaction.

| Propagation | Behavior |
|---|---|
| `REQUIRED` (default) | Join the caller's transaction, else create one |
| `REQUIRES_NEW` | Always starts a new transaction, suspending any existing one |

## Code — `AuditLogService.java` + `EmployeeService.java`

```java
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    // Runs in its own transaction — commits independently of the caller.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String message) {
        AuditLog auditLog = new AuditLog();
        auditLog.setMessage(message);
        auditLogRepository.save(auditLog);
    }
}
```

```java
@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
public Employee updateSalaryWithAudit(Long id, Double salary, boolean simulateFailure) throws Exception {

    Employee employee = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Employee Not Found"));

    employee.setSalary(salary);
    auditLogService.log("Updated salary for employee " + id + " to " + salary);

    if (simulateFailure) {
        throw new Exception("Simulated failure after audit log");
    }

    return employee;
}
```

**Endpoint:** `POST /employee/{id}/salary/audit?salary=90000&simulateFailure=true`

Call it with `simulateFailure=true`: the salary update rolls back (checked `Exception` forced via `rollbackFor`), but the audit row — committed on its own `REQUIRES_NEW` transaction — is still in the `audit_log` table. That's the whole point of `REQUIRES_NEW` for audit logging.

---

# 36. Transaction Isolation

## Definition

Isolation controls how concurrent transactions see each other's uncommitted or changing data.

| Problem | Description |
|---|---|
| Dirty Read | Reading another transaction's uncommitted change |
| Non-Repeatable Read | Re-reading a row and getting a different value |
| Phantom Read | Re-running a query and getting a different row count |

| Isolation Level | Dirty Read | Non-Repeatable Read | Phantom Read |
|---|---|---|---|
| `READ_UNCOMMITTED` | ✅ | ✅ | ✅ |
| `READ_COMMITTED` | ❌ | ✅ | ✅ |
| `REPEATABLE_READ` | ❌ | ❌ | ✅ |
| `SERIALIZABLE` | ❌ | ❌ | ❌ |

MySQL (InnoDB, used by this lab) defaults to `REPEATABLE_READ`. `updateSalaryWithAudit` above explicitly sets `isolation = Isolation.READ_COMMITTED`, overriding that default for just this method.

---

# 37. Rollback Rules

## Definition

By default, Spring rolls back on `RuntimeException`/`Error` but **commits** on checked exceptions unless told otherwise.

```text
Throwable
├── Error
└── Exception
      ├── RuntimeException   → Rollback by default
      └── Checked Exception  → Commit by default
```

`rollbackFor` forces a rollback for a checked exception; `noRollbackFor` does the opposite. `updateSalaryWithAudit` uses `rollbackFor = Exception.class` precisely because it throws a plain checked `Exception` to simulate failure — without `rollbackFor`, Spring would commit the salary update despite the exception.

---

## ⭐ Most Asked Transaction Interview Questions

- What does `@Transactional` actually do at runtime?
- `REQUIRED` vs `REQUIRES_NEW` — walk through `updateSalaryWithAudit` and explain why the audit log survives a rollback.
- What is Transaction Isolation, and what's MySQL's default?
- Why doesn't Spring roll back on a checked exception by default, and how does `rollbackFor` change that?

---

# Interview Revision Summary

| Topic | Key Point |
|---------|-----------|
| JPA | Specification for ORM |
| Hibernate | JPA Implementation |
| Spring Data JPA | Repository Abstraction |
| Entity | Maps Java Object to Database Table |
| EntityManager | Core JPA Interface |
| Persistence Context | First Level Cache |
| Dirty Checking | Automatic Update Detection |
| Flush | Synchronizes Changes to Database |
| Derived Query | Query Generated from Method Name |
| JPQL | Entity-based Query Language |
| Native Query | Database SQL |
| Pagination | Divide Large Result Set |
| Slice | Faster Pagination Without COUNT |
| Sorting | ORDER BY Using Entity Fields |
| Projection | Fetch Required Columns Only |
| Specification | Dynamic Queries Using Criteria API |
| QueryDSL | Type-safe Dynamic Queries |
| EntityManager | Programmatic JPQL & Native SQL |
| Owning Side / `mappedBy` | Which side holds the Foreign Key |
| FetchType | LAZY vs EAGER loading |
| N+1 Problem | 1 + N Queries — fixed by `JOIN FETCH` / `@EntityGraph` |
| Cascade / orphanRemoval | Propagate ops to children / delete on unlink |
| Optimistic Locking | `@Version` prevents lost updates |
| Propagation | How a `@Transactional` method joins or starts a transaction |
| Isolation | How concurrent transactions see each other's changes |
| Rollback Rules | Runtime → rollback by default, Checked → commit unless `rollbackFor` |

---

# Conclusion

Spring Data JPA simplifies database access by combining:

- JPA Specification
- Hibernate ORM
- Spring Repository Abstraction

Understanding the internal architecture, persistence context, query generation, relationship mapping, and transaction management is essential for building scalable and maintainable enterprise applications.

This handbook pairs every concept with its working implementation in `com.interview.labs.jpa.*` — read the definition, then run the endpoint to see it happen.

---

⭐ **If these notes helped you, consider starring the repository!**
