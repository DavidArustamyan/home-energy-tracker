package com.kafka_sc.usage_service.dto;

import lombok.Builder;

@Builder
public record DeviceDto(Long id,
                        String name,
                        String type,
                        String location,
                        Long userId,
                        Double energyConsumed) {
    public DeviceDto withEnergyConsumed(Double energyConsumed) {
        return new DeviceDto(id, name, type, location, userId, energyConsumed);
    }
}
