package com.asg.shipping.linecommission.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Used for both Create and Update.
 * Enforces SRS "mandatory" fields at the API layer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineCommissionRequest {

    @NotNull(message = "linePoid is required")
    private Long linePoid;

    @NotNull(message = "periodFrom is required")
    private LocalDate periodFrom;

    @NotNull(message = "periodTo is required")
    private LocalDate periodTo;

    @NotNull(message = "renewalDate is required")
    private LocalDate renewalDate;

    @NotNull(message = "currencyPoid is required")
    private Long currencyPoid;

    @NotNull(message = "transactionDate is required")
    private LocalDate transactionDate;

    @Size(max = 100, message = "description cannot exceed 100 characters")
    private String description;

    @Size(max = 200, message = "remarks cannot exceed 200 characters")
    private String remarks;

    @Valid
    private List<ContainerRateDto> containerRates = new ArrayList<>();

    @Valid
    private List<OtherRemunerationDto> otherRemunerations = new ArrayList<>();

    @Valid
    private List<LocalShareDto> localShares = new ArrayList<>();
}


