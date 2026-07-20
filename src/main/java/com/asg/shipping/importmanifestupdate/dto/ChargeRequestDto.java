package com.asg.shipping.importmanifestupdate.dto;

import com.asg.shipping.common.dto.LovItem;
import com.asg.shipping.importmanifestupdate.service.BlManifestValidationService;
import lombok.*;

import java.math.BigDecimal;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargeRequestDto implements BlManifestValidationService.ChargeValidatable {
    private Long detRowId;
    private Long chargePoid;
    private LovItem chargeDet;

    private BigDecimal currencyExchange;
    private BigDecimal quantity;
    private BigDecimal buyPercharge;
    private BigDecimal perQuantityAmount;
    private Long paidAtPortPoid;
    private LovItem paidAtPortDet;

    private String chargeType;
    private LovItem chargeTypeDet;
    private String currencyCode;
    private LovItem currencyCodeDet;
    private String freightType;
    private LovItem freightTypeDet;
    private String ediChargeCode;
    private Long arShReceiptTransactionPoid;
    private String chargeBasisOn;
    private LovItem basisDet;
    private String printGroup;
    private Long receiptInvoicePoid;
    private String docRefLinkNo;
    private Long reprintDetRowId;
    private Long reprintTransactionPoid;
    private String invoiceType;
    private String autoCanInvoiceNo;
    private LovItem receiptInvoiceDet;
    private String chargeDescription;
    private Long taxPoid;
    private LovItem taxDet;

    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;

    // Computed display fields (buyPercharge * quantity, perQuantityAmount * quantity)
    private BigDecimal buyAmount;
    private BigDecimal saleAmount;

    private String cnRefDocId;
    private String cnRefDocPoid;
    private String cnRefDetRowId;
    private String cnIssueInvoice;
    private String selectRow;
    private String actionType;

    // ---- ChargeValidatable interface ----
    @Override public Long getChargePoidValue() { return chargePoid; }
    @Override public String getFreightTypeValue() { return freightType; }
    @Override public BigDecimal getQuantityValue() { return quantity; }
    @Override public BigDecimal getSellValue() { return perQuantityAmount; }
    @Override public BigDecimal getBuyValue() { return buyPercharge; }
}
