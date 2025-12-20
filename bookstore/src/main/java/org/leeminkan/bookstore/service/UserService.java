package org.leeminkan.bookstore.service;

import org.leeminkan.bookstore.dto.CreateUserRequest;
import org.leeminkan.bookstore.model.User;
import org.leeminkan.bookstore.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Injected BCrypt bean

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(CreateUserRequest request) {
        // 1. Check if user already exists (business logic)
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already taken!");
        }

        // 2. Map DTO to Entity and Hash the password
        User user = new User();
        user.setUsername(request.getUsername());

        // CRITICAL STEP: Hash the password before saving!
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // 3. Assign a default role
        user.setRole("USER");

        return userRepository.save(user);
    }

    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found after authentication."));
    }
}