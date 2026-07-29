package com.interview.labs.jpa.service;

import com.interview.labs.jpa.dto.EmployeeDto;
import com.interview.labs.jpa.dto.LockerRequestDto;
import com.interview.labs.jpa.entity.Employee;
import com.interview.labs.jpa.entity.Locker;
import com.interview.labs.jpa.entity.Project;
import com.interview.labs.jpa.entity.QEmployee;
import com.interview.labs.jpa.repository.EmployeeRepository;
import com.interview.labs.jpa.repository.LockerRepository;
import com.interview.labs.jpa.repository.ProjectRepository;
import com.interview.labs.jpa.repository.projection.EmployeeView;
import com.interview.labs.jpa.specification.EmployeeSpecification;
import com.querydsl.core.BooleanBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.Query;

import java.util.List;
import java.util.Set;

@Service
public class EmployeeService {

    private final EmployeeRepository repository;

    private final LockerRepository lockerRepository;

    private final ProjectRepository projectRepository;

    private final AuditLogService auditLogService;

    private final EntityManager entityManager;

    private static final Set<String> ALLOWED_COLUMNS =
            Set.of(
                    "name",
                    "salary",
                    "department",
                    "city"
            );

    public EmployeeService(EmployeeRepository repository,
                           LockerRepository lockerRepository,
                           ProjectRepository projectRepository,
                           AuditLogService auditLogService,
                           EntityManager entityManager) {
        this.repository = repository;
        this.lockerRepository = lockerRepository;
        this.projectRepository = projectRepository;
        this.auditLogService = auditLogService;
        this.entityManager = entityManager;
    }

    @Transactional
    public Employee updateEmployee(Employee request) {

        Employee employee = repository.findById(request.getId())
                .orElseThrow(() ->
                        new RuntimeException("Employee Not Found"));

        employee.setSalary(request.getSalary());

        return employee;
    }

    public List<Employee> getEmployees(
            Double salary){

        return repository.findEmployeesNative(
                salary);
    }

    public List<Employee> getEmployeesJpql(Double salary) {

        return repository.findEmployeesBySalary(salary);
    }

    public Page<Employee> getEmployees(
            int page,
            int size){

        Pageable pageable =
                PageRequest.of(page,size);

        return repository.findAll(pageable);

    }

    public Slice<Employee> getEmployees1(
            int page,
            int size) {

        Pageable pageable =
                PageRequest.of(page, size);

        return repository.findAllBy(pageable);

    }

    public List<Employee> getEmployees() {

        return repository.findAll(
                Sort.by("salary").descending());

    }

    public List<EmployeeView> getEmployee(){

        return repository.getEmployeeView();

    }

    public List<EmployeeDto> getEmployeeWithProjection(){

        return repository.findEmployee();

    }

    public List<EmployeeView> getEmployeeWithNativeQueryAndProjection(){

        return repository.getEmployeeViewNative();

    }

    // Derived query passthroughs — no JPQL/SQL written, Spring builds it from the method name.
    public List<Employee> findByDepartmentName(String departmentName) {
        return repository.findByDepartmentName(departmentName);
    }

    public List<Employee> findByCityAndSalaryGreaterThan(String city, Double salary) {
        return repository.findByCityAndSalaryGreaterThan(city, salary);
    }

    public List<Employee> findEmployee(Double salary){

        return repository.findAll(

                EmployeeSpecification
                        .hasSalaryGreaterThan(salary)

        );

    }

    public Iterable<Employee> search(

            Double salary,

            String name){

        QEmployee employee = QEmployee.employee;

        BooleanBuilder builder =
                new BooleanBuilder();

        if(salary!=null){

            builder.and(

                    employee.salary.gt(salary)

            );

        }

        if(name!=null){

            builder.and(
                    employee.name.eq(name)
            );

        }

        return repository.findAll(builder);

    }


    public List<Employee> search(String column, String value) {

        if (!ALLOWED_COLUMNS.contains(column)) {
            throw new IllegalArgumentException("Invalid Column Name");
        }

        String sql = "SELECT * FROM employee WHERE " + column + " = :value";

        Query query = entityManager.createNativeQuery(sql, Employee.class);

        switch (column) {

            case "id" ->
                    query.setParameter("value", Long.parseLong(value));

            case "salary" ->
                    query.setParameter("value", Double.parseDouble(value));

            case "version" ->
                    query.setParameter("value", Integer.parseInt(value));

            case "name", "department", "city" ->
                    query.setParameter("value", value);

            default ->
                    throw new IllegalArgumentException("Unsupported Column");
        }

        return query.getResultList();
    }

    @Transactional
    public Locker assignLocker(LockerRequestDto dto) {

        Employee employee = repository.findById(dto.getEmployeeId())
                .orElseThrow(() ->
                        new RuntimeException("Employee Not Found"));

        Locker locker = lockerRepository.findById(dto.getLockerId())
                .orElse(null);

        if (locker == null) {

            locker = new Locker();
            locker.setId(dto.getLockerId());
        }

        locker.setLockerNumber(dto.getLockerNumber());

        // Owning Side
        locker.setEmployee(employee);

        return lockerRepository.save(locker);
    }

    // Self-invocation demo: called from outside, this goes through the Spring AOP
    // proxy (LoggingAspect's Before/After/Around fire). But the call below to
    // this.updateSalaryWithFlush(...) is a plain Java call on `this` — it bypasses
    // the proxy entirely, so the aspect does NOT fire for it, even though calling
    // /employee/{id}/salary/flush directly does. Same reason @Transactional is
    // silently skipped on self-invocation — it's the same proxy mechanism.
    public Employee selfInvocationDemo(Long id, Double salary) {
        return this.updateSalaryWithFlush(id, salary);
    }

    // Explicit flush() demo: the UPDATE is sent to the database here, but the
    // transaction is still open — it only becomes permanent when this method returns and commits.
    @Transactional
    public Employee updateSalaryWithFlush(Long id, Double salary) {

        Employee employee = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee Not Found"));

        employee.setSalary(salary);
        entityManager.flush();

        return employee;
    }

    // Optimistic locking demo: pass the version you last read back as expectedVersion.
    // If another update has since changed the row, Hibernate's UPDATE ... WHERE id=? AND version=?
    // matches zero rows and throws OptimisticLockException.
    @Transactional
    public Employee updateSalaryOptimistic(Long id, Double salary, Integer expectedVersion) {

        Employee employee = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee Not Found"));

        entityManager.detach(employee);
        employee.setSalary(salary);
        employee.setVersion(expectedVersion);

        try {
            return entityManager.merge(employee);
        } catch (OptimisticLockException ex) {
            throw new IllegalStateException(
                    "Employee was modified by another transaction. Please reload and retry.", ex);
        }
    }

    @Transactional
    public Employee assignProject(Long employeeId, Long projectId) {

        Employee employee = repository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee Not Found"));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project Not Found"));

        employee.getProjects().add(project);

        return employee;
    }

    @Transactional
    public Project createProject(String name) {
        Project project = new Project();
        project.setName(name);
        return projectRepository.save(project);
    }

    // Propagation + isolation + rollbackFor demo: the salary update rolls back on
    // any Exception (rollbackFor), but the audit entry — running with REQUIRES_NEW —
    // commits independently, so it survives even when this method fails.
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
}