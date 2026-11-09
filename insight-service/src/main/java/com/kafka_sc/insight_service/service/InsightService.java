package com.kafka_sc.insight_service.service;

import com.kafka_sc.insight_service.client.UsageClient;
import com.kafka_sc.insight_service.dto.DeviceDto;
import com.kafka_sc.insight_service.dto.InsightDto;
import com.kafka_sc.insight_service.dto.UsageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Slf4j
@Service
public class InsightService {

    private static final int DAYS = 3;

    private final UsageClient usageClient;
    private final ChatClient chatClient;

    public InsightService(UsageClient usageClient, ChatClient chatClient) {
        this.usageClient = usageClient;
        this.chatClient = chatClient;
    }

    public InsightDto getSavingsTips (Long userId) {
        final UsageDto usageData = usageClient.getXDaysUsageForUser(userId, DAYS);
        if (hasNoDevices(usageData)) return noDevicesInsight(userId);
        double totalUsage = totalUsage(usageData);

        String prompt = String.format(Locale.ROOT, """
                                This is my total energy consumption over the past %d days: %.2f.
                                How can I reduce my energy consumption?
                                How does it compare to an average household?
                                """, DAYS, totalUsage
        );
        return askModel(userId, prompt, totalUsage);
    }

    public InsightDto getOverview(Long userId) {
        final UsageDto usageData = usageClient.getXDaysUsageForUser(userId, 3);

        if (hasNoDevices(usageData)) return noDevicesInsight(userId);
        double totalUsage = totalUsage(usageData);

        String prompt = String.format(Locale.ROOT, """
                                Analyse the following energy usage data and provide a concise overview
                                with actionable insights. The data is aggregated over the past %d days.
                                Usage per device:
                                %s
                """, DAYS, usageData.devices());
        return askModel(userId, prompt, totalUsage);
    }

    private InsightDto askModel(Long userId, String prompt, double totalUsage) {
        log.info("Calling LLM for userId {} with total usage {}", userId, totalUsage);
        String tips = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return InsightDto.builder()
                .userId(userId)
                .tips(tips)
                .energyUsage(totalUsage)
                .build();
    }

    private static boolean hasNoDevices(UsageDto usageDto) {
        return usageDto.devices() == null || usageDto.devices().isEmpty();
    }

    private static double totalUsage(UsageDto usageDto) {
        return usageDto.devices().stream().mapToDouble(DeviceDto::energyConsumed).sum();
    }

    private static InsightDto noDevicesInsight(Long userId) {
        return InsightDto.builder()
                .userId(userId)
                .tips("No devices found for this user")
                .energyUsage(0)
                .build();
    }
}
