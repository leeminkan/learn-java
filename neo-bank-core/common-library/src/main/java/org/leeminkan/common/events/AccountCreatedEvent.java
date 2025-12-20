package org.leeminkan.common.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class AccountCreatedEvent extends BaseEvent {
    private Long id;
    private String accountHolderId;
    private BigDecimal initialBalance;
}