package com.kafka_sc.kafka.event;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertingEvent {
    private Long userId;
    private String message;
    private String email;
    private double threshold;
    private double energyConsumed;
}
