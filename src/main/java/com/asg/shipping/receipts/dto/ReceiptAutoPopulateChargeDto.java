package com.asg.shipping.receipts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReceiptAutoPopulateChargeDto {

    private Long blPoid;
    private Long chargePoid;
    private Long detRowId;
    private BigDecimal amount;
    private String addFlag;
    private String invoiceType;
    private Long taxPoid;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
}
