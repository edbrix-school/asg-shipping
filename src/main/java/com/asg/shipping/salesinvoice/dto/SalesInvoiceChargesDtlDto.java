package com.asg.shipping.salesinvoice.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO for Sales Invoice Charges Detail
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesInvoiceChargesDtlDto {

    private Long detRowId;
    private Long blPoid;
    private LovGetListDto blDet;
    private Long chargesDetRowId;
    private Long chargePoid;
    private LovGetListDto chargeDet;
    private BigDecimal amount;
    private String amountSelect;
    private BigDecimal buyAmount;
    private String currencyCode;
    private LovGetListDto currencyDet;
    private BigDecimal currencyExchange;
    private BigDecimal quantity;
    private LocalDate lpoSrnDate;
    private String lpoSrnNo;
    private BigDecimal perQtyBuyAmt;
    private BigDecimal perQtySellAmt;
    private String printGroupTemp;
    private String chargeType;
    private String chargeNewRecord;
    private Long taxPoid;
    private LovGetListDto taxDet;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private String cnRefDocId;
    private String cnRefDocPoid;
    private String cnRefDetRowId;
    private String printCurrencyCode;
    private LovGetListDto printCurrencyDet;
    private BigDecimal printCurrencyExchange;
    private BigDecimal printRateAmt;

    private String actionType;  // "isCreated", "isUpdated", "isDeleted", "noChanges"
}

