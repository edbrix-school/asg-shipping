package com.asg.shipping.importmanifestupdate.dto;

import com.asg.shipping.importManifestUpdate.service.BlManifestValidationService;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargeRequestDto implements BlManifestValidationService.ChargeValidatable {
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
    private String actionType;

    // ---- ChargeValidatable interface ----
    @Override public Long getChargePoidValue() { return chargePoid; }
    @Override public String getFreightTypeValue() { return freightType; }
    @Override public Long getQuantityValue() { return quantity; }
    @Override public Long getSellValue() { return perQuantityAmount; }
    @Override public Long getBuyValue() { return buyPercharge; }
}
