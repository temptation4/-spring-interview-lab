package com.interview.labs.jpa.controller;

import com.interview.labs.jpa.dto.EmployeeDto;
import com.interview.labs.jpa.dto.LockerRequestDto;
import com.interview.labs.jpa.dto.LockerResponse;
import com.interview.labs.jpa.entity.Employee;
import com.interview.labs.jpa.entity.Locker;
import com.interview.labs.jpa.repository.projection.EmployeeView;
import com.interview.labs.jpa.service.EmployeeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employee")
public class EmployeeController {

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @PutMapping
    public Employee updateEmployee(@RequestBody Employee employee) {
        return service.updateEmployee(employee);
    }

    @GetMapping("/salary/{salary}")
    public List<Employee> getEmployees(
            @PathVariable Double salary){

        return service.getEmployees(salary);

    }

    @GetMapping("/page")
    public Page<Employee> getEmployee(

            @RequestParam int page,

            @RequestParam int size){

        return service.getEmployees(page,size);

    }

    @GetMapping("/slice")
    public Slice<Employee> getEmployees(

            @RequestParam int page,

            @RequestParam int size) {

        return service.getEmployees1(page, size);

    }

    @GetMapping("/sort")
    public List<Employee> sort() {

        return service.getEmployees();

    }

    @GetMapping("/projection")
    public List<EmployeeView> projection(){

        return service.getEmployee();

    }

    @GetMapping("/projectionDto")
    public List<EmployeeDto> projectionDto(){

        return service.getEmployeeWithProjection();

    }

    @GetMapping("/projectionNative")
    public List<EmployeeDto> projectionWithNativeQuery(){

        return service.getEmployeeWithNativeQueryAndProjection();

    }


    @GetMapping("/specification")
    public List<Employee> findEmployee(

            @RequestParam Double salary){

        return service.findEmployee(salary);

    }

    @GetMapping("/querydsl")
    public Iterable<Employee> searchEmployees(

            @RequestParam(required = false)
            Double salary,

            @RequestParam(required = false)
            String name) {

        return service.search(salary, name);
    }

    @GetMapping("/search")
    public List<Employee> search(

            @RequestParam String column,

            @RequestParam String value) {

        return service.search(
                column,
                value);

    }

    @PostMapping("/locker")
    public LockerResponse assignLocker(
            @RequestBody LockerRequestDto dto) {

        Locker locker = service.assignLocker(dto);

        return new LockerResponse(
                locker.getId(),
                locker.getLockerNumber(),
                locker.getEmployee().getId(),
                locker.getEmployee().getName()
        );
    }


}
