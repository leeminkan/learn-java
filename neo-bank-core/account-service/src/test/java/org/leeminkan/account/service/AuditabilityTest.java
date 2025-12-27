package org.leeminkan.account.service;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.Test;
import org.leeminkan.account.BaseIntegrationTest;
import org.leeminkan.account.domain.Account;
import org.leeminkan.account.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuditabilityTest extends BaseIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    public void testAuditTrailIsCreated() {
        // 1. Transaction A: Create Account (Revision 1)
        Account account = new Account();
        account.setHolderName("Audited User");
        account.setEmail("audit@bank.com");
        account.setBalance(new BigDecimal("1000.00"));

        // SAVE & CAPTURE (Version 0)
        account = accountRepository.save(account);
        Long accountId = account.getId();

        // 2. Transaction B: Withdraw Money (Revision 2)
        account.setBalance(new BigDecimal("900.00"));

        // FIX: Re-assign 'account' to get Version 1
        account = accountRepository.save(account);

        // 3. Transaction C: Deposit Money (Revision 3)
        account.setBalance(new BigDecimal("1500.00"));

        // FIX: Re-assign 'account' to get Version 2 (though not strictly needed for the last step)
        account = accountRepository.save(account);

        // --- THE AUDIT (Verification) ---

        // We use AuditReader to query the shadow tables (_AUD)
        AuditReader reader = AuditReaderFactory.get(entityManagerFactory.createEntityManager());

        // Get all revisions for this specific Account ID
        List<Number> revisions = reader.getRevisions(Account.class, accountId);

        assertEquals(3, revisions.size(), "Should have 3 distinct versions of history");

        // "Time Travel" to Revision 1
        Account rev1 = reader.find(Account.class, accountId, revisions.get(0));
        assertEquals(new BigDecimal("1000.00").setScale(2), rev1.getBalance().setScale(2));

        // "Time Travel" to Revision 2
        Account rev2 = reader.find(Account.class, accountId, revisions.get(1));
        assertEquals(new BigDecimal("900.00").setScale(2), rev2.getBalance().setScale(2));

        // "Time Travel" to Revision 3 (Current)
        Account rev3 = reader.find(Account.class, accountId, revisions.get(2));
        assertEquals(new BigDecimal("1500.00").setScale(2), rev3.getBalance().setScale(2));

        System.out.println("Audit Passed: Full history of balance changes is preserved.");
    }
}