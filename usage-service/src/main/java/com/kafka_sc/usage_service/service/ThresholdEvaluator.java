package com.kafka_sc.usage_service.service;

import com.kafka_sc.kafka.event.AlertingEvent;
import com.kafka_sc.usage_service.dto.UserDto;
import com.kafka_sc.usage_service.model.DeviceEnergy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ThresholdEvaluator {

    static final String ALERT_MESSAGE = "Energy consumption threshold exceeded";

    public Map<Long, Double> sumByUser(List<DeviceEnergy> deviceEnergies) {
        return deviceEnergies.stream()
                .filter(deviceEnergy -> deviceEnergy.getUserId() != null)
                .collect(Collectors.groupingBy(
                        DeviceEnergy::getUserId,
                        Collectors.summingDouble(DeviceEnergy::getEnergyConsumed)
                ));
    }

    public List<AlertingEvent> evaluate(Map<Long, Double> consumptionByUser, Map<Long, UserDto> users) {
        return consumptionByUser.entrySet().stream()
                .map(entry -> alertIfExceeded(entry.getKey(), entry.getValue(), users.get(entry.getKey())))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<AlertingEvent> alertIfExceeded(Long userId, double consumption, UserDto user) {
        if (user == null || !user.alerting() || consumption <= user.energyAlertingThreshold()) {
            return Optional.empty();
        }
        return Optional.of(AlertingEvent.builder()
                .userId(userId)
                .email(user.email())
                .threshold(user.energyAlertingThreshold())
                .energyConsumed(consumption)
                .message(ALERT_MESSAGE)
                .build());
    }
}
