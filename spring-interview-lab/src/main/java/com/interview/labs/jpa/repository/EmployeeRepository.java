package com.interview.labs.jpa.repository;

import com.interview.labs.jpa.dto.EmployeeDto;
import com.interview.labs.jpa.entity.Employee;
import com.interview.labs.jpa.repository.projection.EmployeeView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmployeeRepository
        extends JpaRepository<Employee, Long>,
        JpaSpecificationExecutor<Employee>, QuerydslPredicateExecutor {

    @Query("""
            SELECT e
            FROM Employee e
            WHERE e.salary > :salary
            """)
    List<Employee> findEmployeesBySalary(@Param("salary") Double salary);

    @Query(value = """
            SELECT *
            FROM employee
            WHERE salary > :salary
            """, nativeQuery = true)
    List<Employee> findEmployeesNative(@Param("salary") Double salary);

    @Query("""
            SELECT
            e.name as name,
            e.salary as salary
            FROM Employee e
            """)
    List<EmployeeView> getEmployeeView();

    @Query("""
            SELECT new com.interview.labs.jpa.dto.EmployeeDto(
                    e.name,
                    e.salary)
            FROM Employee e
            """)
    List<EmployeeDto> findEmployee();

    // Interface projection via an actual native query (the JPQL constructor
    // expression above only works in JPQL, not native SQL) — Spring maps the
    // result set columns onto the EmployeeView getters by name.
    @Query(value = """
            SELECT e.name AS name, e.salary AS salary
            FROM employee e
            """, nativeQuery = true)
    List<EmployeeView> getEmployeeViewNative();

    // findAllBy with no criteria + a Slice return type -> Spring fetches
    // pageSize + 1 rows and skips the COUNT query entirely (true Slice, unlike
    // findAll(pageable), which is a Page under the hood and always counts).
    Slice<Employee> findAllBy(Pageable pageable);

    // Derived queries — Spring generates the JPQL from the method name.
    List<Employee> findByDepartmentName(String departmentName);

    List<Employee> findByCityAndSalaryGreaterThan(String city, Double salary);

    boolean existsByName(String name);
}
