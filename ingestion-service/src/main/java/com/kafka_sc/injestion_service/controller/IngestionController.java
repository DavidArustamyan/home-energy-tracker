package com.kafka_sc.injestion_service.controller;

import com.kafka_sc.injestion_service.dto.EnergyUsageDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.kafka_sc.injestion_service.service.IngestionService
@RestController
@RequestMapping("/api/v1/ingestion")
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void ingestData(@RequestBody EnergyUsageDto usageDto) {
        ingestionService.ingestEnergyUsage(usageDto);
    }
}
