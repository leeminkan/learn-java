package org.leeminkan.transaction.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.leeminkan.common.ratelimit.RateLimit;
import org.leeminkan.transaction.domain.Transaction;
import org.leeminkan.transaction.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    // Allow 5 requests per 60 seconds
    @RateLimit(limit = 5, duration = 60, key = "tx_create")
    public Transaction createTransfer(@RequestBody TransferRequest request) {
        return transactionService.initiateTransfer(
                request.fromAccountId,
                request.toAccountId,
                request.amount
        );
    }

    @Data
    static class TransferRequest {
        private Long fromAccountId;
        private Long toAccountId;
        private BigDecimal amount;
    }
}