package com.kafka_sc.ingestion_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;

import java.time.Instant;

@Builder
public record EnergyUsageDto(
    @NotNull(message = "deviceId is required") @Positive(message = "deviceId must be positive") Long deviceId,
    @PositiveOrZero(message = "energyConsumed must be zero or positive") double energyConsumed,
    @NotNull(message = "timestamp is required") @PastOrPresent(message = "timestamp must not be in the future")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant timestamp
) {}
