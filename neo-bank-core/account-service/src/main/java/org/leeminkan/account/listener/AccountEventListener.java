package org.leeminkan.account.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.leeminkan.account.domain.Account;
import org.leeminkan.account.repository.AccountRepository;
import org.leeminkan.common.events.TransactionInitiatedEvent;
import org.leeminkan.common.events.TransactionProcessedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountEventListener {

    private final AccountRepository accountRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Listen to the topic where Transaction Service sends messages
    @KafkaListener(topics = "transaction-events", groupId = "account-group")
    @Transactional
    public void handleTransactionInitiated(TransactionInitiatedEvent event) {
        log.info("Received transaction request: {}", event);

        // 1. Fetch both accounts
        Account fromAccount = accountRepository.findById(event.getFromAccountId())
                .orElseThrow(() -> new RuntimeException("Sender account not found"));

        Account toAccount = accountRepository.findById(event.getToAccountId())
                .orElseThrow(() -> new RuntimeException("Receiver account not found"));

        // 2. Validate Balance
        if (fromAccount.getBalance().compareTo(event.getAmount()) < 0) {
            log.warn("Insufficient funds for Account ID: {}", fromAccount.getId());
            publishResult(event.getTransactionId(), "FAILED", "Insufficient Funds");
            return;
        }

        // 3. Move the Money
        fromAccount.setBalance(fromAccount.getBalance().subtract(event.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(event.getAmount()));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        log.info("Money moved successfully from {} to {}", fromAccount.getId(), toAccount.getId());

        // 4. Publish Success Event
        publishResult(event.getTransactionId(), "SUCCESS", "Transfer Complete");
    }

    private void publishResult(Long transactionId, String status, String reason) {
        TransactionProcessedEvent resultEvent = TransactionProcessedEvent.builder()
                .transactionId(transactionId)
                .status(status)
                .reason(reason)
                .build();

        // We send this to a NEW topic: "transaction-results"
        kafkaTemplate.send("transaction-results", resultEvent);
    }
}