package org.leeminkan.transaction.client;

import org.leeminkan.transaction.dto.AccountDto; // Import the new DTO
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ACCOUNT-SERVICE")
public interface AccountFeignClient {

    @GetMapping("/api/accounts/{id}")
    AccountDto getAccount(@PathVariable("id") Long id); // Use DTO here
}