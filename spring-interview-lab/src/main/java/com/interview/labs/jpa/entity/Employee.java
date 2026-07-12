package com.interview.labs.jpa.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "employee")
@Data
public class Employee {

    @Id
    private Long id;

    private String name;

    private Double salary;

    private String department;

    private String city;

    @Version
    private Integer version;

}