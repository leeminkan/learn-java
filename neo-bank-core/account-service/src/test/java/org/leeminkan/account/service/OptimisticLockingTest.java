package org.leeminkan.account.service;

import org.junit.jupiter.api.Test;
import org.leeminkan.account.BaseIntegrationTest;
import org.leeminkan.account.domain.Account;
import org.leeminkan.account.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OptimisticLockingTest extends BaseIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    public void testDoubleSpendProtection() throws ExecutionException, InterruptedException {
        // 1. Setup: Create an account with $100 (Version 0)
        Account account = new Account();
        account.setHolderName("Concurrency Tester");
        account.setEmail("concurrent@test.com");
        account.setBalance(new BigDecimal("100.00"));
        account = accountRepository.save(account); // Save triggers ID generation and Version=0
        Long accountId = account.getId();

        System.out.println("--- Setup Complete. Account ID: " + accountId + " Version: " + account.getVersion() + " ---");

        // 2. Simulate two threads reading the SAME data at the SAME time
        // Thread 1 reads Version 0
        Account thread1_Copy = accountRepository.findById(accountId).orElseThrow();
        // Thread 2 reads Version 0 (simulating a race condition)
        Account thread2_Copy = accountRepository.findById(accountId).orElseThrow();

        // 3. Execution Phase

        // Thread 1: Updates balance -> Saves (Version becomes 1)
        CompletableFuture<Void> transaction1 = CompletableFuture.runAsync(() -> {
            System.out.println("Thread 1: Updating balance...");
            thread1_Copy.setBalance(BigDecimal.ZERO);
            accountRepository.save(thread1_Copy);
            System.out.println("Thread 1: Success!");
        });

        // Thread 2: Tries to update using stale data (Version 0) -> Should Fail
        CompletableFuture<Void> transaction2 = CompletableFuture.runAsync(() -> {
            // Wait a tiny bit to ensure Thread 1 finishes first
            try { Thread.sleep(200); } catch (InterruptedException e) {}

            System.out.println("Thread 2: Trying to update stale data...");
            thread2_Copy.setBalance(new BigDecimal("50.00"));
            accountRepository.save(thread2_Copy); // DB Check: Expect Version 0, but DB has 1!
        });

        // 4. Assertions
        transaction1.get(); // Wait for 1 to finish cleanly

        boolean conflictDetected = false;
        try {
            transaction2.get(); // Wait for 2 (Expect Exception)
        } catch (ExecutionException e) {
            // The actual exception is wrapped in ExecutionException
            if (e.getCause() instanceof ObjectOptimisticLockingFailureException) {
                conflictDetected = true;
                System.out.println("Thread 2: Failed as expected! " + e.getCause().getClass().getSimpleName());
            } else {
                e.printStackTrace();
            }
        }

        assertTrue(conflictDetected, "Optimistic Locking failed! The second transaction should have been rejected.");

        // 5. Verify Final State
        Account finalState = accountRepository.findById(accountId).orElseThrow();
        // Version should be 1 (only Thread 1 succeeded)
        // Note: If version starts at 0, 1st save makes it 0, update makes it 1.
        // Depending on Hibernate config it might start at 1. We just check logic holds.
        System.out.println("Final Version: " + finalState.getVersion());
        assertEquals(BigDecimal.ZERO.setScale(2), finalState.getBalance().setScale(2), "Balance should reflect Thread 1's update only");
    }
}