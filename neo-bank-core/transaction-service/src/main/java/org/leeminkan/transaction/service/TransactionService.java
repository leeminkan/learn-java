package org.leeminkan.transaction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.leeminkan.common.events.TransactionInitiatedEvent;
import org.leeminkan.transaction.client.AccountFeignClient;
import org.leeminkan.transaction.domain.Transaction;
import org.leeminkan.transaction.domain.TransactionStatus;
import org.leeminkan.transaction.dto.AccountDto;
import org.leeminkan.transaction.repository.TransactionRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.tracing.Tracer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Tracer tracer;

    // REPLACE THIS:
    // private final AccountValidationClient validationClient;
    // WITH THIS:
    private final AccountFeignClient accountFeignClient;

    @Transactional
    public Transaction initiateTransfer(Long fromId, Long toId, BigDecimal amount) {
        // --- FEIGN VALIDATION ---
        log.info("Validating account {} via Feign...", fromId);
        try {
            // This single line does discovery, load balancing, and the HTTP call
            AccountDto account = accountFeignClient.getAccount(fromId);
            log.info("Account found: {} (Balance: {})", account.getHolderName(), account.getBalance());
        } catch (Exception e) {
            // Feign throws FeignException.NotFound if 404, or RetryableException if down
            log.error("Account validation failed", e);
            throw new RuntimeException("Transfer Denied: Account validation failed");
        }
        // ------------------------

        if (tracer.currentSpan() != null) {
            tracer.currentSpan().tag("bank.from_account", fromId.toString());
        }

        // 1. Save Transaction as PENDING
        Transaction transaction = Transaction.builder()
                .fromAccountId(fromId)
                .toAccountId(toId)
                .amount(amount)
                .status(TransactionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Transaction savedTx = transactionRepository.save(transaction);

        // 2. Publish Event to Kafka
        TransactionInitiatedEvent event = TransactionInitiatedEvent.builder()
                .transactionId(savedTx.getId())
                .fromAccountId(fromId)
                .toAccountId(toId)
                .amount(amount)
                .build();

        kafkaTemplate.send("transaction-events", event);
        log.info("Transaction initiated: {}", event);

        return savedTx;
    }
}