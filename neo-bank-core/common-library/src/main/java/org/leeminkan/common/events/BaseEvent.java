package org.leeminkan.common.events;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@SuperBuilder
public class BaseEvent {
    private String eventId = UUID.randomUUID().toString();
    private LocalDateTime eventDate = LocalDateTime.now();
}