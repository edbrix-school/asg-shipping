package com.asg.shipping.exportManifestUpdate.dto;

import com.asg.shipping.common.dto.LovItem;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for Charge Details.
 *
 * String @Size limits mirror the column widths on SHIP_BL_MANIFEST_CHARGES_DTL.
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

    @Size(max = 25, message = "Charge Type must not exceed 25 characters")
    private String chargeType;
    private LovItem chargeTypeDet;

    @Size(max = 50, message = "Currency Code must not exceed 50 characters")
    private String currencyCode;

    @Size(max = 25, message = "Freight Type must not exceed 25 characters")
    private String freightType;
    private LovItem freightTypeDet;

    @Size(max = 50, message = "EDI Charge Code must not exceed 50 characters")
    private String ediChargeCode;
    private Long arShReceiptTransactionPoid;

    @Size(max = 20, message = "Charge Basis On must not exceed 20 characters")
    private String chargeBasisOn;
    private LovItem chargeBasisOnDet;

    @Size(max = 50, message = "Print Group must not exceed 50 characters")
    private String printGroup;
    private Long receiptInvoicePoid;
    private LovItem receiptInvoiceDet; // LOV: MANIFEST_RECEIPT_INVOICE

    @Size(max = 100, message = "Doc Ref Link No must not exceed 100 characters")
    private String docRefLinkNo;
    private Long reprintDetRowId;
    private Long reprintTransactionPoid;

    @Size(max = 100, message = "Invoice Type must not exceed 100 characters")
    private String invoiceType;

    @Size(max = 200, message = "Auto Can Invoice No must not exceed 200 characters")
    private String autoCanInvoiceNo;

    @Size(max = 200, message = "Charge Description must not exceed 200 characters")
    private String chargeDescription;
    private Long taxPoid;
    private LovItem taxDet; // LOV: TAX_MASTER
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;

    @Size(max = 100, message = "CN Ref Doc Id must not exceed 100 characters")
    private String cnRefDocId;

    @Size(max = 300, message = "CN Ref Doc Poid must not exceed 300 characters")
    private String cnRefDocPoid;

    @Size(max = 300, message = "CN Ref Det Row Id must not exceed 300 characters")
    private String cnRefDetRowId;

    @Size(max = 100, message = "CN Issue Invoice must not exceed 100 characters")
    private String cnIssueInvoice;

    @Size(max = 1, message = "Select Row must be a single character")
    private String selectRow;

    // Calculated fields (not in DB, calculated in service)
    private BigDecimal buyAmount; // quantity * buyPercharge
    private BigDecimal saleAmount; // quantity * perQuantityAmount
    private BigDecimal revenue; // saleAmount - buyAmount
    private String drilldownLinkInfo; // For display purposes
    private ActionType actionType;
}
