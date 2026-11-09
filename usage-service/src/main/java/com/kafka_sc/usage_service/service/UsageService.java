package com.kafka_sc.usage_service.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.kafka_sc.kafka.event.AlertingEvent;
import com.kafka_sc.kafka.event.EnergyUsageEvent;
import com.kafka_sc.usage_service.client.DeviceClient;
import com.kafka_sc.usage_service.client.UserClient;
import com.kafka_sc.usage_service.dto.DeviceDto;
import com.kafka_sc.usage_service.dto.UsageDto;
import com.kafka_sc.usage_service.dto.UserDto;
import com.kafka_sc.usage_service.exception.UsageDataUnavailableException;
import com.kafka_sc.usage_service.model.Device;
import com.kafka_sc.usage_service.model.DeviceEnergy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UsageService {

    private final InfluxDBClient influxDBClient;
    private final DeviceClient deviceClient;
    private final UserClient userClient;
    private final KafkaTemplate<String, AlertingEvent> kafkaTemplate;
    private final ThresholdEvaluator thresholdEvaluator;

    @Value("${influx.bucket}")
    private String influxBucket;

    @Value("${influx.org}")
    private String influxOrg;


    public UsageService(InfluxDBClient influxDBClient,
                        DeviceClient deviceClient,
                        UserClient userClient,
                        KafkaTemplate<String, AlertingEvent> kafkaTemplate,
                        ThresholdEvaluator thresholdEvaluator) {
        this.influxDBClient = influxDBClient;
        this.deviceClient = deviceClient;
        this.userClient = userClient;
        this.kafkaTemplate = kafkaTemplate;
        this.thresholdEvaluator = thresholdEvaluator;
    }

    @KafkaListener(topics = "energy-usage", groupId = "usage-service")
    public void energyUsageEvent(EnergyUsageEvent energyUsageEvent) {
        // log.info("Received energy usage event: {}", energyUsageEvent);
        Point point = Point.measurement("energy_usage")
                .addTag("deviceId", String.valueOf(energyUsageEvent.deviceId()))
                .addField("energyConsumed", energyUsageEvent.energyConsumed())
                .time(energyUsageEvent.timestamp(), WritePrecision.MS);
        influxDBClient.getWriteApiBlocking().writePoint(influxBucket, influxOrg, point);
    }

    @Scheduled(cron = "*/10 * * * * *")
    public void aggregateDeviceEnergyUsage() {
        List<DeviceEnergy> deviceEnergies = fetchLastHourUsagePerDevice();
        assignOwners(deviceEnergies);

        Map<Long, Double> consumptionByUser =thresholdEvaluator.sumByUser(deviceEnergies);
        Map<Long, UserDto> users = fetchUsers(consumptionByUser.keySet());

        List<AlertingEvent> alerts = thresholdEvaluator.evaluate(consumptionByUser, users);
        alerts.forEach(alert -> {
            log.info("ALERT: user {} consumed {} in the last hour, threshold {}",
                    alert.userId(), alert.energyConsumed(), alert.userId());
            kafkaTemplate.send("energy-alerts", alert);
        });
    }

    private List<DeviceEnergy> fetchLastHourUsagePerDevice() {
        final Instant now = Instant.now();
        final Instant oneHourAgo = now.minusSeconds(3600);

        String fluxQuery = String.format("""
            from(bucket: "%s")
              |> range(start: time(v: "%s"), stop: time(v: "%s"))
              |> filter(fn: (r) => r["_measurement"] == "energy_usage")
              |> filter(fn: (r) => r["_field"] == "energyConsumed")
              |> group(columns: ["deviceId"])
              |> sum(column: "_value")
        """, influxBucket, oneHourAgo.toString(), now);

        QueryApi queryApi = influxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(fluxQuery, influxOrg);

        List<DeviceEnergy> deviceEnergies = new ArrayList<>();

        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                String deviceIdStr = (String) record.getValueByKey("deviceId");
                Double energyConsumed = record.getValueByKey("_value") instanceof Number ?
                        ((Number) record.getValueByKey("_value")).doubleValue() : 0.0;

                deviceEnergies.add(
                        DeviceEnergy.builder()
                                .deviceId(Long.valueOf(deviceIdStr))
                                .energyConsumed(energyConsumed)
                                .build()
                );
            }
        }
        log.info("Aggregated device energies over the past hour: {}", deviceEnergies);
        return deviceEnergies;
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

    public UsageDto getXDaysUsageForUser(Long userId, int days) {
        log.info("Getting usage for userId: {} over past {} days", userId, days);
        final List<DeviceDto> devicesDto = deviceClient.getAllDevicesForUser(userId);

        final List<Device> devices = new ArrayList<>();

        for (DeviceDto deviceDto : devicesDto) {
            devices.add(Device.builder()
                    .id(deviceDto.id())
                    .name(deviceDto.name())
                    .type(deviceDto.type())
                    .location(deviceDto.location())
                    .userId(deviceDto.userId())
                    .build());
        }

        if (devices == null || devices.isEmpty()) {
            return UsageDto.builder()
                    .userId(userId)
                    .devices(List.of())
                    .build();
        }

        List<String> deviceIdStrings = devices.stream()
                .map(Device::getId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toList();

        final Instant now = Instant.now();
        final Instant start = now.minusSeconds((long) days * 24 * 3600);

        final String deviceFilter = deviceIdStrings.stream()
                .map(idStr -> String.format("r[\"deviceId\"] == \"%s\"", idStr))
                .collect(Collectors.joining(" or "));

        String fluxQuery = String.format("""
        from(bucket: "%s")
          |> range(start: time(v: "%s"), stop: time(v: "%s"))
          |> filter(fn: (r) => r["_measurement"] == "energy_usage")
          |> filter(fn: (r) => r["_field"] == "energyConsumed")
          |> filter(fn: (r) => %s)
          |> group(columns: ["deviceId"])
          |> sum(column: "_value")
        """, influxBucket, start.toString(), now.toString(), deviceFilter);

        final  Map<Long, Double> aggregatedMap = new HashMap<>();

        try {
            QueryApi queryApi = influxDBClient.getQueryApi();
            List<FluxTable> tables = queryApi.query(fluxQuery, influxOrg);

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    Object deviceIdObj = record.getValueByKey("deviceId");
                    String deviceIdStr = deviceIdObj == null ? null : deviceIdObj.toString();
                    if (deviceIdStr == null) continue;

                    Double energyConsumed = record.getValueByKey("_value") instanceof Number
                            ? ((Number) record.getValueByKey("_value")).doubleValue()
                            : 0.0;

                    try {
                        Long deviceId = Long.valueOf(deviceIdStr);
                        aggregatedMap.put(deviceId, aggregatedMap.getOrDefault(deviceId, 0.0) + energyConsumed);
                    } catch (NumberFormatException nfe) {
                        log.warn("Failed to parse deviceId from flux record: {}", deviceIdStr);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to query InfluxDB for user {} usage over {} days", userId, days, e);
            throw new UsageDataUnavailableException(e);
        }

        for (Device device : devices) {
            if (device == null || device.getId() == null) continue;
            device.setEnergyConsumed(aggregatedMap.getOrDefault(device.getId(), 0.0));
        }

        log.info("Aggregated energy consumption for userId {}: {}", userId, aggregatedMap);

        final List<DeviceDto> resultDevices = devices.stream()
                .map(d -> DeviceDto.builder()
                        .id(d.getId())
                        .name(d.getName())
                        .type(d.getType())
                        .location(d.getLocation())
                        .userId(d.getUserId())
                        .energyConsumed(d.getEnergyConsumed())
                        .build())
                .toList();

        return UsageDto.builder()
                .userId(userId)
                .devices(resultDevices)
                .build();
    }
}
