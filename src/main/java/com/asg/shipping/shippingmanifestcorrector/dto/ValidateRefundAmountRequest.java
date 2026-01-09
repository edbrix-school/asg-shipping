package com.asg.shipping.shippingmanifestcorrector.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for validating refund amounts
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidateRefundAmountRequest {

    @NotNull(message = "BL POID is required")
    private Long blPoid;

    @NotNull(message = "Container number is required")
    @NotBlank(message = "Container number is required")
    private String containerNumber;

    private BigDecimal revPayable;
    private BigDecimal revIncome;
    private BigDecimal perQuantityAmount;
}

