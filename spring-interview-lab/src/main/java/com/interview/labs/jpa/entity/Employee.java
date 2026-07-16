package com.interview.labs.jpa.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "employee")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    private Long id;

    private String name;

    private Double salary;

    private String department;

    private String city;

    @Version
    private Integer version;

    @OneToOne(mappedBy = "employee")
    private Locker locker;
}