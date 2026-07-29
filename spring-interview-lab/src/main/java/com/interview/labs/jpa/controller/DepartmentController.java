package com.interview.labs.jpa.controller;

import com.interview.labs.jpa.entity.Department;
import com.interview.labs.jpa.entity.Employee;
import com.interview.labs.jpa.service.DepartmentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/department")
public class DepartmentController {

    private final DepartmentService service;

    public DepartmentController(DepartmentService service) {
        this.service = service;
    }

    @PostMapping
    public Department createDepartment(@RequestParam String name) {
        return service.createDepartment(name);
    }

    @PutMapping("/{departmentId}/employee/{employeeId}")
    public Employee assignEmployee(
            @PathVariable Long departmentId,
            @PathVariable Long employeeId) {

        return service.assignEmployeeToDepartment(departmentId, employeeId);
    }

    @DeleteMapping("/{departmentId}/employee/{employeeId}")
    public void removeEmployee(
            @PathVariable Long departmentId,
            @PathVariable Long employeeId) {

        service.removeEmployeeFromDepartment(departmentId, employeeId);
    }

    @GetMapping("/n-plus-one")
    public List<Department> nPlusOneDemo() {
        return service.getDepartmentsNPlusOne();
    }

    @GetMapping("/join-fetch")
    public List<Department> joinFetchDemo() {
        return service.getDepartmentsJoinFetch();
    }

    @GetMapping("/entity-graph")
    public List<Department> entityGraphDemo() {
        return service.getDepartmentsEntityGraph();
    }
}
