package com.asg.shipping.salesinvoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for invoice print detail data returned from PROC_SHIP_BL_PRINT_LOAD
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoicePrintDetailDto {

    private Long detRowId;
    private Long blPoid;
    private String lineChargeDescription;
    private String quantity;
    private String orgCurrencyCode;
    private BigDecimal orgCurrencyExchange;
    private String currencyCode;
    private BigDecimal currencyExchange;
    private BigDecimal perQtySellAmt;
    private BigDecimal perQtyUsdAmt;
    private BigDecimal totalUsd;
}

