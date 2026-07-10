package com.interview.labs.mvc.controller;

import com.interview.labs.mvc.dto.UserRequest;
import com.interview.labs.mvc.dto.UserResponse;
import com.interview.labs.mvc.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public UserResponse getUser(
            @PathVariable Long id) {

        return userService.getUser(id);
    }

    @PostMapping
    public UserResponse createUser(
            @Valid
            @RequestBody UserRequest request) {

        return userService.save(request);
    }

    @GetMapping
    public String searchUser(
            @RequestParam String name) {

        return "Searching : " + name;
    }
}
