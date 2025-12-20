package org.leeminkan.bookstore.controller;

import org.leeminkan.bookstore.dto.CreateUserRequest;
import org.leeminkan.bookstore.dto.JwtResponse;
import org.leeminkan.bookstore.dto.LoginRequest;
import org.leeminkan.bookstore.dto.RefreshTokenRequest;
import org.leeminkan.bookstore.exception.TokenRefreshException;
import org.leeminkan.bookstore.model.RefreshToken;
import org.leeminkan.bookstore.model.User;
import org.leeminkan.bookstore.security.JwtCore;
import org.leeminkan.bookstore.service.RefreshTokenService;
import org.leeminkan.bookstore.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager; // Inject the manager
    private final JwtCore jwtCore; // Inject the utility
    private final RefreshTokenService refreshTokenService; // Inject the service

    // Update constructor:
    public AuthController(UserService userService, AuthenticationManager authenticationManager, JwtCore jwtCore, RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtCore = jwtCore;
        this.refreshTokenService = refreshTokenService; // CRITICAL INJECTION
    }

    // Maps to POST /auth/register
    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@Valid @RequestBody CreateUserRequest request) {
        User savedUser = userService.registerUser(request);
        // Returns the user object (excluding the password) with 201 Created status
        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }

    // Maps to POST /auth/login
    // Maps to POST /auth/login
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        // ... (Authentication logic remains the same) ...

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);


        // Get Spring Security User principal
        org.springframework.security.core.userdetails.User principal =
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

        // Extract roles, removing the "ROLE_" prefix that Spring adds
                List<String> roles = principal.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(role -> role.replace("ROLE_", ""))
                        .collect(Collectors.toList());

        // 1. Generate the short-lived Access Token (JWT)
        String accessToken = jwtCore.generateToken(principal.getUsername(), roles);

        // 2. Generate and save the long-lived Refresh Token
        // Retrieve the user ID from the principal
        String username = authentication.getName();
        User user = userService.findUserByUsername(username);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        // 3. Return both tokens in the DTO
        return ResponseEntity.ok(new JwtResponse(accessToken, refreshToken.getToken(),"Bearer"));
    }

    @PostMapping("/refreshtoken")
    public ResponseEntity<JwtResponse> refreshToken(@RequestBody RefreshTokenRequest request) {

        String requestRefreshToken = request.getRefreshToken();

        // 1. Find the old token and verify its expiration (using service methods)
        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)

                // 2. Map the valid token to the User entity
                .map(token -> {
                    User user = token.getUser();

                    // 3. Generate a new Access Token (JWT)
                    String newAccessToken = jwtCore.generateToken(user.getUsername(), Collections.singletonList(user.getRole()));

                    // 4. Delete the old Refresh Token (CRITICAL STEP FOR SECURITY ROTATION)
                    refreshTokenService.deleteToken(token);

                    // 5. Generate and save a brand new Refresh Token to replace the old one
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

                    // 6. Return the new tokens
                    return ResponseEntity.ok(new JwtResponse(newAccessToken, newRefreshToken.getToken(), "Bearer"));
                })
                // 7. If the token is not found/expired, return the FORBIDDEN exception (from service)
                .orElseThrow(() -> new TokenRefreshException(requestRefreshToken,
                        "Refresh token is not in database or is invalid."));
    }
}