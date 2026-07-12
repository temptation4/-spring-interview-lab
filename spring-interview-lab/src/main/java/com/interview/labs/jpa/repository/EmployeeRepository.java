package com.interview.labs.jpa.repository;

import com.interview.labs.jpa.dto.EmployeeDto;
import com.interview.labs.jpa.entity.Employee;
import com.interview.labs.jpa.repository.projection.EmployeeView;
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

    @Query("""
            SELECT new com.interview.labs.jpa.dto.EmployeeDto(
                    e.name,
                    e.salary)
            FROM Employee e
            """)
    List<EmployeeDto> getEmployeWithNativeQueryandProjection();
}
