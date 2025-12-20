package org.leeminkan.account.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.leeminkan.account.domain.Account;
import org.leeminkan.account.repository.AccountRepository;
import org.leeminkan.common.events.AccountCreatedEvent;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor // Auto-injects constructor dependencies
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional // Ensures DB save is atomic
    public Account createAccount(String holderName, String email, BigDecimal initialBalance) {
        // 1. Save to Database
        Account account = Account.builder()
                .holderName(holderName)
                .email(email)
                .balance(initialBalance)
                .createdAt(LocalDateTime.now())
                .build();

        Account savedAccount = accountRepository.save(account);
        log.info("Account created in DB: ID={}", savedAccount.getId());

        // 2. Create Event Payload
        AccountCreatedEvent event = AccountCreatedEvent.builder()
                .id(savedAccount.getId())
                .accountHolderId(savedAccount.getEmail())
                .initialBalance(savedAccount.getBalance())
                .build();

        // 3. Publish to Kafka Topic "account-events"
        // kafkaTemplate.send("account-events", event);
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send("account-events", event);
        future
                .thenAccept(result -> {
                    // Hành động này chỉ chạy sau khi Kafka Broker xác nhận đã nhận tin
                    log.info("Xác nhận đã gửi tin thành công: {}", result.getRecordMetadata());
                })
                .exceptionally(ex -> {
                    // Xử lý lỗi nếu việc gửi thất bại (ví dụ: mất kết nối)
                    log.error("Gửi tin Kafka thất bại: {}", ex.getMessage());
                    return null;
                });

        log.info("Event published to Kafka: {}", event);


        return savedAccount;
    }

    public Account getAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }
}