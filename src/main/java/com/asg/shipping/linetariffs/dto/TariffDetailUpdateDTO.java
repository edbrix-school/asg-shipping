package com.asg.shipping.linetariffs.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for updating tariff detail records
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TariffDetailUpdateDTO {

    private Long detRowId; // Null for new records, present for existing records

    private String actionType; // "isDeleted" to delete, empty/null to keep

    @NotNull(message = "Container type is required")
    @jakarta.validation.constraints.Positive(message = "Container type is required")
    private Long containerTypePoid;

    @PositiveOrZero(message = "Free days must be positive or zero")
    private Integer freeDays;

    @PositiveOrZero(message = "Slab 1 till days must be positive or zero")
    private Integer slab1Tilldays;

    private BigDecimal slab1Rate;

    @PositiveOrZero(message = "Slab 2 till days must be positive or zero")
    private Integer slab2Tilldays;

    private BigDecimal slab2Rate;

    @PositiveOrZero(message = "Slab 3 till days must be positive or zero")
    private Integer slab3Tilldays;

    private BigDecimal slab3Rate;

    @PositiveOrZero(message = "Slab 4 till days must be positive or zero")
    private Integer slab4Tilldays;

    private BigDecimal slab4Rate;

    @PositiveOrZero(message = "Slab 5 till days must be positive or zero")
    private Integer slab5Tilldays;

    private BigDecimal slab5Rate;

    @PositiveOrZero(message = "Slab 6 till days must be positive or zero")
    private Integer slab6Tilldays;

    private BigDecimal slab6Rate;

    @PositiveOrZero(message = "Slab 7 till days must be positive or zero")
    private Integer slab7Tilldays;

    private BigDecimal slab7Rate;
}

