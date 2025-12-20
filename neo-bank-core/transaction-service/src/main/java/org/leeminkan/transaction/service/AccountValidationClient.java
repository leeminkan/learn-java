package org.leeminkan.transaction.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j
public class AccountValidationClient {

    private final RestClient restClient;

    public AccountValidationClient(RestClient.Builder builder,
                                   @Value("${account.service.url:http://localhost:8081}") String accountServiceUrl) {
        this.restClient = builder.baseUrl(accountServiceUrl).build();
    }

    @CircuitBreaker(name = "account-validation", fallbackMethod = "fallbackValidation")
    public boolean validateAccount(Long accountId) {
        try {
            // We assume Account Service has a GET /api/accounts/{id} endpoint
            // If it returns 200 OK, the account is valid.
            restClient.get()
                    .uri("/api/accounts/" + accountId)
                    .retrieve()
                    .toBodilessEntity();

            return true;
        } catch (RestClientException e) {
            // If 404 or connection refused, we consider it invalid or throw error
            throw e;
        }
    }

    // FALLBACK METHOD
    // Must have same signature as original + Throwable
    public boolean fallbackValidation(Long accountId, Throwable t) {
        log.warn("Circuit Breaker Open! Account Service is unreachable. Reason: {}", t.getMessage());
        // Logic: Should we allow the transfer if we can't check the account?
        // For a bank: NO. Block it.
        return false;
    }
}