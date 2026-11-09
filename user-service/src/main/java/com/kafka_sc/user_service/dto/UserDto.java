package com.kafka_sc.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private Long id;
    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name must be at most 100 characters")
    private String name;
    @Size(max = 100, message = "surname must be at most 100 characters")
    private String surname;
    @NotBlank(message = "email is required")
    @Email(message = "must be valid email address")
    @Size(max = 255, message = "email must be at most 255 characters")
    private String email;
    private String address;
    private boolean alerting;
    @PositiveOrZero(message = "energyAlertingThreshold must be 0 or more")
    private double energyAlertingThreshold;
}
