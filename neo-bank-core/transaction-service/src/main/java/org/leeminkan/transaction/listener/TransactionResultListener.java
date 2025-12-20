package org.leeminkan.transaction.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.leeminkan.common.events.TransactionProcessedEvent;
import org.leeminkan.transaction.domain.Transaction;
import org.leeminkan.transaction.domain.TransactionStatus;
import org.leeminkan.transaction.repository.TransactionRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionResultListener {

    private final TransactionRepository transactionRepository;

    // Listen to the "transaction-results" topic
    @KafkaListener(topics = "transaction-results", groupId = "transaction-group")
    @Transactional
    public void handleTransactionResult(TransactionProcessedEvent event) {
        log.info("Received result for Transaction ID: {}", event.getTransactionId());

        Transaction transaction = transactionRepository.findById(event.getTransactionId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        // Update the status based on the event
        if ("SUCCESS".equals(event.getStatus())) {
            transaction.setStatus(TransactionStatus.SUCCESS);
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            log.error("Transaction failed: {}", event.getReason());
        }

        transactionRepository.save(transaction);
        log.info("Transaction {} updated to status: {}", transaction.getId(), transaction.getStatus());
    }
}