package org.leeminkan.account.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.leeminkan.account.domain.Account;
import org.leeminkan.account.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Account createAccount(@RequestBody @Valid CreateAccountRequest request) {
        return accountService.createAccount(
                request.holderName,
                request.email,
                request.initialBalance
        );
    }

    // A simple internal DTO class
    @Data
    static class CreateAccountRequest {
        private String holderName;
        @Email
        private String email;
        @PositiveOrZero(message = "Balance cannot be negative")
        private BigDecimal initialBalance;
    }

    @GetMapping("/{id}")
    public Account getAccount(@PathVariable Long id) {
        return accountService.getAccount(id);
    }
}