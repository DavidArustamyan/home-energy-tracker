package com.kafka_sc.user_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDto {
    private Long id;
    private String name;
    private String surName;
    private String email;
    private String address;
    private boolean alerting;
    private double energyAlertingThreshold;
}
