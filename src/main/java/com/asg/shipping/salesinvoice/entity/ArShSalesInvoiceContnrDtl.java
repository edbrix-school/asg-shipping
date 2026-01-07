package com.asg.shipping.salesinvoice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.asg.common.lib.utility.ASGHelperUtils.*;

/**
 * Entity class for AR_SH_SALES_INVOICE_CONTNR_DTL table
 */
@Entity
@Table(name = "AR_SH_SALES_INVOICE_CONTNR_DTL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@IdClass(ArShSalesInvoiceContnrDtlId.class)
public class ArShSalesInvoiceContnrDtl {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "BL_POID")
    private Long blPoid;

    @Column(name = "CONTAINER_SOC_YN", length = 1)
    private String containerSocYn;

    @Column(name = "CONTAINER_NO", length = 25)
    private String containerNo;

    @Column(name = "DM_FRM_DATE")
    private LocalDate dmFrmDate;

    @Column(name = "DM_TO_DATE")
    private LocalDate dmToDate;

    @Column(name = "DM_DAYS")
    private Integer dmDays;

    @Column(name = "DM_CHARGE_AMT", precision = 18, scale = 3)
    private BigDecimal dmChargeAmt;

    @Column(name = "FREE_DAYS", precision = 10, scale = 0)
    private Integer freeDays;

    @Column(name = "EQUIPMENT_ISO_TYPE", length = 20)
    private String equipmentIsoType;

    @Column(name = "DLV_FORM_PRINTED", length = 25)
    @Builder.Default
    private String dlvFormPrinted = "N";

    @Column(name = "RTN_FORM_PRINTED", length = 25)
    @Builder.Default
    private String rtnFormPrinted = "N";

    @Column(name = "EMPTY_IN")
    private LocalDate emptyIn;

    @Column(name = "CNT_TAX_POID")
    private Long cntTaxPoid;

    @Column(name = "CNT_TAX_PERCENTAGE", precision = 18, scale = 3)
    private BigDecimal cntTaxPercentage;

    @Column(name = "CNT_TAX_AMOUNT", precision = 18, scale = 3)
    private BigDecimal cntTaxAmount;

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
        if (dlvFormPrinted == null) {
            dlvFormPrinted = "N";
        }
        if (rtnFormPrinted == null) {
            rtnFormPrinted = "N";
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

