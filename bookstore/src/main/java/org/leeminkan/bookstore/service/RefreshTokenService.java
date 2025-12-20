package org.leeminkan.bookstore.service;

import org.leeminkan.bookstore.exception.TokenRefreshException;
import org.leeminkan.bookstore.model.RefreshToken;
import org.leeminkan.bookstore.model.User;
import org.leeminkan.bookstore.repository.RefreshTokenRepository;
import org.leeminkan.bookstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID; // The Java utility class!

@Service
public class RefreshTokenService {

    @Value("${jwt.refreshExpiration.ms}") // We'll assume a new property for refresh token lifetime
    private long refreshExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository; // Need to access User repository

    // Constructor Injection
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new RuntimeException("User not found for token creation."));

        // 1. Check if token already exists for this user
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUser(user);

        RefreshToken token;

        if (existingToken.isPresent()) {
            // 2. TOKEN EXISTS: Update the existing token's value and expiration
            token = existingToken.get();
            token.setToken(UUID.randomUUID().toString()); // Generate a new UUID value
            token.setExpiryDate(Instant.now().plusMillis(refreshExpirationMs));
        } else {
            // 3. TOKEN DOES NOT EXIST: Create a new token entity
            token = new RefreshToken();
            token.setUser(user);
            token.setToken(UUID.randomUUID().toString());
            token.setExpiryDate(Instant.now().plusMillis(refreshExpirationMs));
        }

        // 4. Save/Update the token
        return refreshTokenRepository.save(token);
    }

    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        // CHECK 2: Expiration Check
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            // The Token is expired! Delete it from the database and throw an exception.
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token was expired. Please make a new sign-in request.");
        }
        return token;
    }

    // This is the core logic that the controller will call
    public Optional<RefreshToken> findByToken(String token) {
        // CHECK 1: Authenticity/Existence Check
        return refreshTokenRepository.findByToken(token);
    }

    public void deleteToken(RefreshToken token) {
        refreshTokenRepository.delete(token);
    }
}