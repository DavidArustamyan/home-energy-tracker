package com.kafka_sc.usage_service.controller;

import com.kafka_sc.usage_service.dto.UsageDto;
import com.kafka_sc.usage_service.service.UsageService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/usage")
public class UsageController {
    private final UsageService usageService;

    public UsageController(UsageService usageService) {
        this.usageService = usageService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UsageDto> getUserDeviceUsage(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "3") @Min(1) @Max(30) int days) {
        final UsageDto usage = usageService.getXDaysUsageForUser(userId, days);
        return ResponseEntity.ok(usage);
    }
}
