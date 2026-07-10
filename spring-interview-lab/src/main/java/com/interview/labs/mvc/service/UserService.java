package com.interview.labs.mvc.service;

import com.interview.labs.mvc.dto.UserRequest;
import com.interview.labs.mvc.dto.UserResponse;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    public UserResponse getUser(Long id) {

        return new UserResponse(
                id,
                "Neelu",
                "neelu@gmail.com");
    }

    public UserResponse save(UserRequest request) {

        return new UserResponse(
                1L,
                request.getName(),
                request.getEmail());
    }
}
