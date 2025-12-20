package org.leeminkan.account.repository;

import org.leeminkan.account.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    // Basic CRUD is already included!
    // We can add custom queries here if needed, e.g.:
    // Optional<Account> findByEmail(String email);
}