package com.asg.shipping.exportManifestUpdate.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for charge totals
 */
@Data
public class ChargeTotalsDto {
    private BigDecimal totalDtlBuyAmt;
    private BigDecimal totalDtlSellAmt;
    private BigDecimal totalDtlGainAmt;
    private BigDecimal totalDtlVatAmt;
}

