package org.leeminkan.bookstore.controller;

import org.leeminkan.bookstore.dto.UserResponse;
import org.leeminkan.bookstore.mapper.UserMapper;
import org.leeminkan.bookstore.model.User;
import org.leeminkan.bookstore.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    // Inject service and mapper
    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    // Maps to GET /api/users/me (Requires Authentication)
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        // 1. Get the username from the authentication principal
        String username = authentication.getName();

        // 2. Look up the full User Entity
        User user = userService.findUserByUsername(username);

        // 3. Map the sensitive Entity to the safe DTO and return
        UserResponse response = userMapper.toResponse(user);

        return ResponseEntity.ok(response);
    }
}