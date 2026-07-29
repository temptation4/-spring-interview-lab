package com.interview.labs.jpa.repository;

import com.interview.labs.jpa.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}
