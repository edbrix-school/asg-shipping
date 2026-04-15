package com.asg.shipping.portstoragetariffsmaster.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for updating a tariff detail record
 * Note: detRowId is included to identify existing records for update
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TariffDetailUpdateDTO {

    private Long detRowId; // If provided, update existing; if null, create new

    private String actionType;

    private Long containerTypePoid;

    @DecimalMin(value = "0.0", inclusive = true, message = "Container size must be non-negative")
    @Digits(integer = 7, fraction = 3, message = "Container size must have at most 7 integer digits and 3 decimal places")
    private BigDecimal containerSize;

    @Positive(message = "Free days must be positive")
    private Integer freeDays;

    @Positive(message = "Slab 1 till days must be positive")
    private Integer slab1Tilldays;

    @DecimalMin(value = "0.0", inclusive = true, message = "Slab 1 rate must be non-negative")
    @Digits(integer = 16, fraction = 2, message = "Slab 1 rate must have at most 16 integer digits and 2 decimal places")
    private BigDecimal slab1Rate;

    @Positive(message = "Slab 2 till days must be positive")
    private Integer slab2Tilldays;

    @DecimalMin(value = "0.0", inclusive = true, message = "Slab 2 rate must be non-negative")
    @Digits(integer = 16, fraction = 2, message = "Slab 2 rate must have at most 16 integer digits and 2 decimal places")
    private BigDecimal slab2Rate;

    @Positive(message = "Slab 3 till days must be positive")
    private Integer slab3Tilldays;

    @DecimalMin(value = "0.0", inclusive = true, message = "Slab 3 rate must be non-negative")
    @Digits(integer = 16, fraction = 2, message = "Slab 3 rate must have at most 16 integer digits and 2 decimal places")
    private BigDecimal slab3Rate;

    @Positive(message = "Slab 4 till days must be positive")
    private Integer slab4Tilldays;

    @DecimalMin(value = "0.0", inclusive = true, message = "Slab 4 rate must be non-negative")
    @Digits(integer = 16, fraction = 2, message = "Slab 4 rate must have at most 16 integer digits and 2 decimal places")
    private BigDecimal slab4Rate;

    @Positive(message = "Slab 5 till days must be positive")
    private Integer slab5Tilldays;

    @DecimalMin(value = "0.0", inclusive = true, message = "Slab 5 rate must be non-negative")
    @Digits(integer = 16, fraction = 2, message = "Slab 5 rate must have at most 16 integer digits and 2 decimal places")
    private BigDecimal slab5Rate;

    @Positive(message = "Slab 6 till days must be positive")
    private Integer slab6Tilldays;

    @DecimalMin(value = "0.0", inclusive = true, message = "Slab 6 rate must be non-negative")
    @Digits(integer = 16, fraction = 2, message = "Slab 6 rate must have at most 16 integer digits and 2 decimal places")
    private BigDecimal slab6Rate;

    @Positive(message = "Slab 7 till days must be positive")
    private Integer slab7Tilldays;

    @DecimalMin(value = "0.0", inclusive = true, message = "Slab 7 rate must be non-negative")
    @Digits(integer = 16, fraction = 2, message = "Slab 7 rate must have at most 16 integer digits and 2 decimal places")
    private BigDecimal slab7Rate;
}