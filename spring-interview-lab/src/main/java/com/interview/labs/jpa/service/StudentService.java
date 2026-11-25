package com.interview.labs.jpa.service;

import com.interview.labs.jpa.dto.BookResponse;
import com.interview.labs.jpa.dto.StudentResponse;
import com.interview.labs.jpa.entity.Student;
import com.interview.labs.jpa.repository.StudentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getAllStudents() {

        return studentRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    private StudentResponse convertToDTO(Student student) {

        StudentResponse response = new StudentResponse();

        response.setId(student.getId());
        response.setName(student.getName());

        List<BookResponse> books = student.getBooks()
                .stream()
                .map(book -> {
                    BookResponse bookResponse = new BookResponse();
                    bookResponse.setId(book.getId());
                    bookResponse.setTitle(book.getTitle());
                    bookResponse.setAuthor(book.getAuthor());
                    return bookResponse;
                })
                .toList();

        response.setBooks(books);

        return response;
    }
}