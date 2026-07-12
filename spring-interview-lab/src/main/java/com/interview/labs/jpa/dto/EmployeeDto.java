package com.interview.labs.jpa.dto;

import lombok.Data;

@Data
public class EmployeeDto {

    private String name;

    private Double salary;

    public EmployeeDto(String name, Double salary) {
        this.name = name;
        this.salary = salary;
    }
}