package com.asg.shipping.exportManifestUpdate.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity class for SHIP_BL_MANIFEST_CHARGES_DTL table
 */
@Entity
@Table(name = "SHIP_BL_MANIFEST_CHARGES_DTL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ExportShipBlManifestChargesDtlId.class)
public class ExportShipBlManifestChargesDtl extends BaseEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "CHARGE_POID")
    private Long chargePoid;

    @Column(name = "CURRENCY_EXCHANGE", precision = 25, scale = 3)
    private BigDecimal currencyExchange;

    @Column(name = "QUANTITY", nullable = false, precision = 25, scale = 3)
    private BigDecimal quantity;

    @Column(name = "BUY_PERCHARGE", precision = 25, scale = 3)
    private BigDecimal buyPercharge;

    @Column(name = "PER_QUANTITY_AMOUNT", precision = 25, scale = 3)
    private BigDecimal perQuantityAmount;

    @Column(name = "PAID_AT_PORT_POID")
    private Long paidAtPortPoid;

    @Column(name = "CHARGE_TYPE", nullable = false, length = 25)
    private String chargeType;

    @Column(name = "CURRENCY_CODE", length = 50)
    private String currencyCode;

    @Column(name = "FREIGHT_TYPE", nullable = false, length = 25)
    private String freightType;

    @Column(name = "EDI_CHARGE_CODE", length = 50)
    private String ediChargeCode;

    @Column(name = "AR_SH_RECEIPT_TRANSACTION_POID")
    private Long arShReceiptTransactionPoid;

    @Column(name = "CHARGE_BASIS_ON", length = 20)
    private String chargeBasisOn;

    @Column(name = "PRINT_GROUP", length = 50)
    private String printGroup;

    @Column(name = "RECEIPT_INVOICE_POID")
    private Long receiptInvoicePoid;

    @Column(name = "DOC_REF_LINK_NO", length = 100)
    private String docRefLinkNo;

    @Column(name = "REPRINT_DET_ROW_ID")
    private Long reprintDetRowId;

    @Column(name = "REPRINT_TRANSACTION_POID")
    private Long reprintTransactionPoid;

    @Column(name = "INVOICE_TYPE", length = 100)
    private String invoiceType;

    @Column(name = "AUTO_CAN_INVOICE_NO", length = 200)
    private String autoCanInvoiceNo;

    @Column(name = "CHARGE_DESCRIPTION", length = 200)
    private String chargeDescription;

    @Column(name = "TAX_POID")
    private Long taxPoid;

    @Column(name = "TAX_PERCENTAGE", precision = 25, scale = 3)
    private BigDecimal taxPercentage;

    @Column(name = "TAX_AMOUNT", precision = 25, scale = 3)
    private BigDecimal taxAmount;

    @Column(name = "CN_REF_DOC_ID", length = 100)
    private String cnRefDocId;

    @Column(name = "CN_REF_DOC_POID", length = 300)
    private String cnRefDocPoid;

    @Column(name = "CN_REF_DET_ROW_ID", length = 300)
    private String cnRefDetRowId;

    @Column(name = "CN_ISSUE_INVOICE", length = 100)
    private String cnIssueInvoice;

    @Column(name = "SELECT_ROW", length = 1)
    private String selectRow;

    @PrePersist
    protected void onCreate() {
        if (chargeType == null) {
            chargeType = "MANIFEST";
        }
        if (invoiceType == null) {
            invoiceType = "MANUAL";
        }
        // Trigger logic: If PER_QUANTITY_AMOUNT is 0 and BUY_PERCHARGE > 0, set PER_QUANTITY_AMOUNT = BUY_PERCHARGE
        if (chargeType != null && chargeType.equals("MANIFEST") && 
            perQuantityAmount != null && perQuantityAmount.compareTo(BigDecimal.ZERO) == 0 &&
            buyPercharge != null && buyPercharge.compareTo(BigDecimal.ZERO) > 0) {
            perQuantityAmount = buyPercharge;
        }
    }

}

