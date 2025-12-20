package org.leeminkan.bookstore.repository;

import org.leeminkan.bookstore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Spring implements: SELECT * FROM app_user WHERE username = ?
    Optional<User> findByUsername(String username);
}