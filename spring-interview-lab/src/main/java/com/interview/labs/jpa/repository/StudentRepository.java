package com.interview.labs.jpa.repository;

import com.interview.labs.jpa.entity.Student;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StudentRepository
        extends JpaRepository<Student, Long> {

    @Query("""
        SELECT DISTINCT s
        FROM Student s
        LEFT JOIN FETCH s.books
        """)
    List<Student> findAllWithBooks();

    @EntityGraph(attributePaths = {"books"})
    List<Student> findAll();
}
