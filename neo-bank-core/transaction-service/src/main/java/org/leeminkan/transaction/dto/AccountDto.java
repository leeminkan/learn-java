package org.leeminkan.transaction.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AccountDto {
    private Long id;
    private String holderName;
    private BigDecimal balance;
}