package org.leeminkan.common.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class TransactionProcessedEvent extends BaseEvent {
    private Long transactionId;
    private String status; // "SUCCESS" or "FAILED"
    private String reason; // e.g., "Insufficient Funds"
}