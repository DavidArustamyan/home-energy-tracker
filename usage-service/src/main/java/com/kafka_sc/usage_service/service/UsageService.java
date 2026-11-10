package com.kafka_sc.usage_service.service;

import com.influxdb.client.InfluxDBClient;
import com.kafka_sc.kafka.event.AlertingEvent;
import com.kafka_sc.kafka.event.EnergyUsageEvent;
import com.kafka_sc.usage_service.client.DeviceClient;
import com.kafka_sc.usage_service.client.UserClient;
import com.kafka_sc.usage_service.dto.DeviceDto;
import com.kafka_sc.usage_service.dto.UsageDto;
import com.kafka_sc.usage_service.dto.UserDto;
import com.kafka_sc.usage_service.model.DeviceEnergy;
import com.kafka_sc.usage_service.repository.EnergyUsageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@Slf4j
public class UsageService {

    private final EnergyUsageRepository energyUsageRepository;
    private final DeviceClient deviceClient;
    private final UserClient userClient;
    private final KafkaTemplate<String, AlertingEvent> kafkaTemplate;
    private final ThresholdEvaluator thresholdEvaluator;

    public UsageService(DeviceClient deviceClient,
                        UserClient userClient,
                        KafkaTemplate<String, AlertingEvent> kafkaTemplate,
                        ThresholdEvaluator thresholdEvaluator,
                        EnergyUsageRepository energyUsageRepository) {
        this.deviceClient = deviceClient;
        this.userClient = userClient;
        this.kafkaTemplate = kafkaTemplate;
        this.thresholdEvaluator = thresholdEvaluator;
        this.energyUsageRepository = energyUsageRepository;
    }

    @KafkaListener(topics = "energy-usage", groupId = "usage-service")
    public void energyUsageEvent(EnergyUsageEvent energyUsageEvent) {
        energyUsageRepository.save(energyUsageEvent);
    }

    @Scheduled(cron = "*/10 * * * * *")
    public void aggregateDeviceEnergyUsage() {
        List<DeviceEnergy> deviceEnergies = fetchLastHourUsagePerDevice();
        assignOwners(deviceEnergies);

        Map<Long, Double> consumptionByUser = thresholdEvaluator.sumByUser(deviceEnergies);
        Map<Long, UserDto> users = fetchUsers(consumptionByUser.keySet());

        List<AlertingEvent> alerts = thresholdEvaluator.evaluate(consumptionByUser, users);
        alerts.forEach(alert -> {
            log.info("ALERT: user {} consumed {} in the last hour, threshold {}",
                    alert.userId(), alert.energyConsumed(), alert.threshold());
            kafkaTemplate.send("energy-alerts", alert);
        });
    }

    public UsageDto getXDaysUsageForUser(Long userId, int days) {
        final List<DeviceDto> devices = deviceClient.getAllDevicesForUser(userId);
        if (devices.isEmpty()) return UsageDto.builder().userId(userId).devices(List.of()).build();

        List<Long> deviceIds = devices.stream().map(DeviceDto::id).toList();
        Instant now = Instant.now();
        Map<Long, Double> consumptionByDevice = energyUsageRepository.sumByDevice(now.minus(Duration.ofDays(days)), now, deviceIds);

        List<DeviceDto> devicesWithUsage = devices.stream()
                .map(device -> device.withEnergyConsumed(consumptionByDevice.getOrDefault(device.id(), 0.0))).toList();

        return UsageDto.builder().userId(userId).devices(devicesWithUsage).build();
    }

    private List<DeviceEnergy> fetchLastHourUsagePerDevice() {
        final Instant now = Instant.now();
        return energyUsageRepository.sumByDevice(now.minus(Duration.ofHours(1)), now).entrySet().stream()
                .map(entry -> DeviceEnergy.builder()
                        .deviceId(entry.getKey())
                        .energyConsumed(entry.getValue())
                        .build())
                .toList();
    }

    private void assignOwners(List<DeviceEnergy> deviceEnergies) {
        for (DeviceEnergy deviceEnergy : deviceEnergies) {
            try {
                DeviceDto device = deviceClient.getDeviceById(deviceEnergy.getDeviceId());
                if (device != null) {
                    deviceEnergy.setUserId(device.userId());
                }
            } catch (RestClientException e) {
                log.warn("Failed to fetch device {}: {}", deviceEnergy.getDeviceId(), e.getMessage());
            }
        }
    }

    private Map<Long, UserDto> fetchUsers(Set<Long> userIds) {
        Map<Long, UserDto> users = new HashMap<>();
        for (Long userId: userIds) {
            try {
                UserDto user = userClient.getUserById(userId);
                if (user != null) {
                    users.put(userId, user);
                }
            } catch (RestClientException e) {
                log.warn("Failed to fetch user {}: {}", userId, e.getMessage());
            }
        }
        return users;
    }
}
