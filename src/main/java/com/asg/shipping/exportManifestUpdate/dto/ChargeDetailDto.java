package com.asg.shipping.exportManifestUpdate.dto;

import com.asg.shipping.common.dto.LovItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for Charge Details
 */
@Data
public class ChargeDetailDto {
    private Long detRowId;
    private Long chargePoid;
    private LovItem chargeDet; // LOV: CHARGE_MASTER
    private BigDecimal currencyExchange;
    private BigDecimal quantity;
    private BigDecimal buyPercharge;
    private BigDecimal perQuantityAmount;
    private Long paidAtPortPoid;
    private LovItem paidAtPortDet; // LOV: PORT_MASTER
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private String chargeType;
    private String currencyCode;
    private String freightType;
    private String ediChargeCode;
    private Long arShReceiptTransactionPoid;
    private String chargeBasisOn;
    private String printGroup;
    private Long receiptInvoicePoid;
    private LovItem receiptInvoiceDet; // LOV: MANIFEST_RECEIPT_INVOICE
    private String docRefLinkNo;
    private Long reprintDetRowId;
    private Long reprintTransactionPoid;
    private String invoiceType;
    private String autoCanInvoiceNo;
    private String chargeDescription;
    private Long taxPoid;
    private LovItem taxDet; // LOV: TAX_MASTER
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private String cnRefDocId;
    private String cnRefDocPoid;
    private String cnRefDetRowId;
    private String cnIssueInvoice;
    private String selectRow;
    
    // Calculated fields (not in DB, calculated in service)
    private BigDecimal buyAmount; // quantity * buyPercharge
    private BigDecimal saleAmount; // quantity * perQuantityAmount
    private BigDecimal revenue; // saleAmount - buyAmount
    private String drilldownLinkInfo; // For display purposes
}

