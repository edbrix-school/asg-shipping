package com.asg.shipping.shippingmanifestcorrector.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for refund amount validation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidateRefundAmountResponse {

    private Boolean valid;
    private BigDecimal oldPayableAmt;
    private BigDecimal oldIncomeAmt;
    private BigDecimal calculatedBuyPercharge;
    private BigDecimal calculatedPerQuantityAmount;
    private String message;
}

