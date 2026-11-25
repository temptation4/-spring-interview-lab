package com.interview.labs.jpa.repository;

import com.interview.labs.jpa.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository
        extends JpaRepository<Book, Long> {
}
