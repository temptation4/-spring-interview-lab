package com.interview.labs.jpa.service;

import com.interview.labs.jpa.entity.Department;
import com.interview.labs.jpa.entity.Employee;
import com.interview.labs.jpa.repository.DepartmentRepository;
import com.interview.labs.jpa.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    private final EmployeeRepository employeeRepository;

    public DepartmentService(DepartmentRepository departmentRepository, EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public Department createDepartment(String name) {
        Department department = new Department();
        department.setName(name);
        return departmentRepository.save(department);
    }

    // Owning side (Employee.department) is what Hibernate persists — keeping
    // the inverse-side collection in sync just avoids a stale in-memory view.
    @Transactional
    public Employee assignEmployeeToDepartment(Long departmentId, Long employeeId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department Not Found"));

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee Not Found"));

        employee.setDepartment(department);
        department.getEmployees().add(employee);

        return employee;
    }

    // orphanRemoval = true on Department.employees means dropping an Employee
    // from this collection deletes that Employee row entirely, not just the FK.
    @Transactional
    public void removeEmployeeFromDepartment(Long departmentId, Long employeeId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department Not Found"));

        department.getEmployees().removeIf(employee -> employee.getId().equals(employeeId));
    }

    // N+1 demo: 1 query for departments, then 1 query per department to
    // initialize its LAZY employees collection.
    public List<Department> getDepartmentsNPlusOne() {
        List<Department> departments = departmentRepository.findAll();
        departments.forEach(department -> department.getEmployees().size());
        return departments;
    }

    // Fix #1: single SQL query via JOIN FETCH.
    public List<Department> getDepartmentsJoinFetch() {
        return departmentRepository.findAllWithEmployeesJoinFetch();
    }

    // Fix #2: single SQL query via a query-specific EntityGraph.
    public List<Department> getDepartmentsEntityGraph() {
        return departmentRepository.findAllWithEmployeesEntityGraph();
    }
}
