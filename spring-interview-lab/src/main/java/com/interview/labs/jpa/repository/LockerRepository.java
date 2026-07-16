package com.interview.labs.jpa.repository;

import com.interview.labs.jpa.entity.Locker;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LockerRepository
        extends JpaRepository<Locker, Long> {
}