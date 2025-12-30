package com.asg.shipping.importManifestUpdate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargeRequestDto {
    private Long detRowId;
    private Long chargePoid;

    private Long currencyExchange;
    private Long quantity;
    private Long buyPercharge;
    private Long perQuantityAmount;
    private Long paidAtPortPoid;

    private String chargeType;
    private String currencyCode;
    private String freightType;
    private String ediChargeCode;
    private Long arShReceiptTransactionPoid;
    private String chargeBasisOn;
    private String printGroup;
    private Long receiptInvoicePoid;
    private String docRefLinkNo;
    private Long reprintDetRowId;
    private Long reprintTransactionPoid;
    private String invoiceType;
    private String autoCanInvoiceNo;
    private String chargeDescription;
    private Long taxPoid;

    private Long taxPercentage;
    private Long taxAmount;
    private String cnRefDocId;
    private String cnRefDocPoid;
    private String cnRefDetRowId;
    private String cnIssueInvoice;
    private String selectRow;
}
