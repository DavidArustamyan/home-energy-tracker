package com.kafka_sc.usage_service.service;

import com.kafka_sc.kafka.event.AlertingEvent;
import com.kafka_sc.usage_service.dto.UserDto;
import com.kafka_sc.usage_service.model.DeviceEnergy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ThresholdEvaluatorTest {

    private final ThresholdEvaluator thresholdEvaluator = new ThresholdEvaluator();

    @Test
    void sumByUser_sumsDevicesPerUser_andIgnoresDevicesWithoutOwner() {
        List<DeviceEnergy> energies = List.of(
                deviceEnergy(1L, 10.0, 100L),
                deviceEnergy(2L, 5.5, 100L),
                deviceEnergy(3L, 7.0, 200L),
                deviceEnergy(4L, 99.0, null));

        assertThat(thresholdEvaluator.sumByUser(energies)).containsExactlyInAnyOrderEntriesOf(Map.of(100L,15.5, 200L, 7.0));
    }

    @Test
    void evaluate_consumptionAboveThreshold_createsAlert() {
        Map<Long, UserDto> users = Map.of(100L, user(100L, true, 1000.0));
        List<AlertingEvent> alerts = thresholdEvaluator.evaluate(Map.of(100L, 1200.0), users);

        assertThat(alerts).singleElement().satisfies(alert -> {
            assertThat(alert.userId()).isEqualTo(100L);
            assertThat(alert.email()).isEqualTo("user100@example.com");
            assertThat(alert.threshold()).isEqualTo(1000.0);
            assertThat(alert.energyConsumed()).isEqualTo(1200.0);
        });
    }

    @Test
    void evaluate_consumptionAboveThresholdButAlertingIsFalse_createsNoAlert() {
        Map<Long, UserDto> users = Map.of(100L, user(100L, false, 1000.0));
        assertThat(thresholdEvaluator.evaluate(Map.of(100L, 1200.0), users)).isEmpty();
    }

    @Test
    void evaluate_consumptionEqualToThreshold_createsNoAlert() {
        Map<Long, UserDto> users = Map.of(100L, user(100L, true, 1000.0));

        assertThat(thresholdEvaluator.evaluate(Map.of(100L, 1000.0), users)).isEmpty();
    }

    @Test
    void evaluate_consumptionIsLessThanThreshold_createsNoAlert() {
        Map<Long, UserDto> users = Map.of(100L, user(100L, true, 1000.0));

        assertThat(thresholdEvaluator.evaluate(Map.of(100L, 1.0), users)).isEmpty();
    }

    @Test
    void evaluate_noAnswerFromUserService_createsNoAlert() {
        assertThat(thresholdEvaluator.evaluate(Map.of(100L, 1200.0), Map.of())).isEmpty();
    }

    @Test
    void evaluate_twoOutOfThreeUsersAboveThreshold_createsExactlyTwoAlerts() {
        Map<Long, UserDto> users = Map.of(
                100L, user(100L, true, 1000.0),
                200L, user(200L, true, 1000.0),
                300L, user(300L, true, 1000.0));
        Map<Long, Double> consumption = Map.of(
                100L, 1500.0,
                200L, 500.0,
                300L, 2000.0);
        List<AlertingEvent> alerts = thresholdEvaluator.evaluate(consumption, users);

        assertThat(alerts)
                .extracting(AlertingEvent::userId)
                .containsExactlyInAnyOrder(100L, 300L);

    }

    @Test
    void evaluate_emptyUsersList_createsEmptyListNoExceptionThrown() {
        assertThat(thresholdEvaluator.evaluate(Map.of(), Map.of())).isEmpty();

    }

    private static DeviceEnergy deviceEnergy(Long deviceId, double energy, Long userId) {
        return DeviceEnergy.builder().deviceId(deviceId).energyConsumed(energy).userId(userId).build();
    }

    private static UserDto user(Long id, boolean alerting, double threshold) {
        return UserDto.builder()
                .id(id)
                .email("user" + id + "@example.com")
                .alerting(alerting)
                .energyAlertingThreshold(threshold)
                .build();
    }

}
