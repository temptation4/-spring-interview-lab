package com.interview.labs.jpa.controller;

import com.interview.labs.jpa.dto.StudentResponse;
import com.interview.labs.jpa.entity.Student;
import com.interview.labs.jpa.service.StudentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/student")
    public List<StudentResponse> getAllStudents() {
        return studentService.getAllStudents();
    }
}