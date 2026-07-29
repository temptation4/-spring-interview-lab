package com.interview.labs.jpa.repository;

import com.interview.labs.jpa.entity.Department;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    // findAll() is inherited and used as-is for the N+1 demo — accessing
    // department.getEmployees() afterwards triggers one query per department.

    @Query("SELECT d FROM Department d JOIN FETCH d.employees")
    List<Department> findAllWithEmployeesJoinFetch();

    @EntityGraph(attributePaths = "employees")
    @Query("SELECT d FROM Department d")
    List<Department> findAllWithEmployeesEntityGraph();
}
