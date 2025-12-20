package org.leeminkan.bookstore.repository;

import org.leeminkan.bookstore.model.RefreshToken;
import org.leeminkan.bookstore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByUser(User user); // Find a token by the User object
}