package com.interview.labs.jpa.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "locker")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Locker {

    @Id
    private Long id;

    @Column(name = "locker_number")
    private String lockerNumber;

    @OneToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;
}