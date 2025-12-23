package com.asg.shipping.portstoragetariffsmaster.dto;


import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating a new Port Storage Tariff
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortStorageTariffCreateDTO {

    @NotNull(message = "Port is required")
    private Long portPoid;

    @NotBlank(message = "Description is required")
    @Size(max = 100, message = "Description must not exceed 100 characters")
    private String description;

    @NotBlank(message = "Tariff type is required")
    @Size(max = 50, message = "Tariff type must not exceed 50 characters")
    private String tariffType;

    @NotNull(message = "Period from date is required")
    private LocalDate periodFrom;

    @NotNull(message = "Period to date is required")
    private LocalDate periodTo;

    private LocalDate transactionDate;

    @Size(max = 25, message = "Document reference must not exceed 25 characters")
    private String docRef;

    private Long companyPoid;

    @Valid
    private List<TariffDetailCreateDTO> tariffDetails; // Optional detail records
}

