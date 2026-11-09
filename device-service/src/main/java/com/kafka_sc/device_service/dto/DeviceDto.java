package com.kafka_sc.device_service.dto;

import com.kafka_sc.device_service.model.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class DeviceDto {
    private Long id;
    @NotBlank(message = "name is required")
    @Size(max = 255, message = "name must be at most 255 characters")
    private String name;
    @NotNull(message = "type is required")
    private DeviceType type;
    @Size(max = 255, message = "location must be at most 255 characters")
    private String location;
    @NotNull(message = "userId is required")
    @Positive(message = "userId should be positive number")
    private Long userId;
}
