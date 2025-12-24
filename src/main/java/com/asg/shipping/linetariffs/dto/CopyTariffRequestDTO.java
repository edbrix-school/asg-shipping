package com.asg.shipping.linetariffs.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for copying a tariff to new period
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CopyTariffRequestDTO {

    @NotNull(message = "Period from date is required")
    private LocalDate periodFrom;

    @NotNull(message = "Period to date is required")
    private LocalDate periodTo;

    @Size(max = 100, message = "Description must not exceed 100 characters")
    private String description;
}

