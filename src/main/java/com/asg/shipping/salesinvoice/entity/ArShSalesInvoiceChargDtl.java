package com.asg.shipping.salesinvoice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.asg.common.lib.utility.ASGHelperUtils.*;

/**
 * Entity class for AR_SH_SALES_INVOICE_CHARG_DTL table
 */
@Entity
@Table(name = "AR_SH_SALES_INVOICE_CHARG_DTL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@IdClass(ArShSalesInvoiceChargDtlId.class)
public class ArShSalesInvoiceChargDtl {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "BL_POID")
    private Long blPoid;

    @Column(name = "CHARGES_DET_ROW_ID")
    private Long chargesDetRowId;

    @Column(name = "CHARGE_POID")
    private Long chargePoid;

    @Column(name = "AMOUNT", precision = 18, scale = 3)
    private BigDecimal amount;

    @Column(name = "AMOUNT_SELECT", length = 25)
    private String amountSelect;

    @Column(name = "BUY_AMOUNT", precision = 18, scale = 3)
    private BigDecimal buyAmount;

    @Column(name = "CURRENCY_CODE", length = 50)
    private String currencyCode;

    @Column(name = "CURRENCY_EXCHANGE", precision = 18, scale = 6)
    private BigDecimal currencyExchange;

    @Column(name = "QUANTITY", precision = 18, scale = 3)
    private BigDecimal quantity;

    @Column(name = "LPO_SRN_DATE")
    private LocalDate lpoSrnDate;

    @Column(name = "LPO_SRN_NO", length = 25)
    private String lpoSrnNo;

    @Column(name = "PER_QTY_BUY_AMT", precision = 18, scale = 3)
    private BigDecimal perQtyBuyAmt;

    @Column(name = "PER_QTY_SELL_AMT", precision = 18, scale = 3)
    private BigDecimal perQtySellAmt;

    @Column(name = "PRINT_GROUP_TEMP", length = 50)
    private String printGroupTemp;

    @Column(name = "CHARGE_TYPE", length = 50)
    private String chargeType;

    @Column(name = "CHARGE_NEW_RECORD", length = 1)
    @Builder.Default
    private String chargeNewRecord = "N";

    @Column(name = "TAX_POID")
    private Long taxPoid;

    @Column(name = "TAX_PERCENTAGE", precision = 18, scale = 3)
    private BigDecimal taxPercentage;

    @Column(name = "TAX_AMOUNT", precision = 18, scale = 3)
    private BigDecimal taxAmount;

    @Column(name = "CN_REF_DOC_ID", length = 100)
    private String cnRefDocId;

    @Column(name = "CN_REF_DOC_POID", length = 300)
    private String cnRefDocPoid;

    @Column(name = "CN_REF_DET_ROW_ID", length = 300)
    private String cnRefDetRowId;

    @Column(name = "PRINT_CURRENCY_CODE", length = 50)
    private String printCurrencyCode;

    @Column(name = "PRINT_CURRENCY_EXCHANGE", precision = 18, scale = 6)
    private BigDecimal printCurrencyExchange;

    @Column(name = "PRINT_RATE_AMT", precision = 18, scale = 3)
    private BigDecimal printRateAmt;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID", insertable = false, updatable = false)
    private ArShSalesInvoiceHdr header;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (createdBy == null) {
            createdBy = getCurrentUser();
        }
        if (chargeNewRecord == null) {
            chargeNewRecord = "N";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = LocalDateTime.now();
        if (lastModifiedBy == null) {
            lastModifiedBy = getCurrentUser();
        }
    }
}

