package com.interview.labs.jpa.service;

import com.interview.labs.jpa.dto.EmployeeDto;
import com.interview.labs.jpa.dto.LockerRequestDto;
import com.interview.labs.jpa.entity.Employee;
import com.interview.labs.jpa.entity.Locker;
import com.interview.labs.jpa.entity.QEmployee;
import com.interview.labs.jpa.repository.EmployeeRepository;
import com.interview.labs.jpa.repository.LockerRepository;
import com.interview.labs.jpa.repository.projection.EmployeeView;
import com.interview.labs.jpa.specification.EmployeeSpecification;
import com.querydsl.core.BooleanBuilder;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.util.List;
import java.util.Set;

@Service
public class EmployeeService {

    private final EmployeeRepository repository;

    private final LockerRepository lockerRepository;

    private final EntityManager entityManager;

    private static final Set<String> ALLOWED_COLUMNS =
            Set.of(
                    "name",
                    "salary",
                    "department",
                    "city"
            );

    public EmployeeService(EmployeeRepository repository, LockerRepository lockerRepository, EntityManager entityManager) {
        this.repository = repository;
        this.lockerRepository = lockerRepository;
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

        return repository.findAll(pageable);

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

    public List<EmployeeDto> getEmployeeWithNativeQueryAndProjection(){

        return repository.getEmployeWithNativeQueryandProjection();

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
}