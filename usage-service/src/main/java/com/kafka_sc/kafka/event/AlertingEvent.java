package com.kafka_sc.kafka.event;

import lombok.Builder;

@Builder
public record AlertingEvent(Long userId,
                            String message,
                            String email,
                            double threshold,
                            double energyConsumed
) {
}
