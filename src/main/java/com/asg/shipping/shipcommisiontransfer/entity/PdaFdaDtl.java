package com.asg.shipping.shipcommisiontransfer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "PDA_FDA_DTL")
public class PdaFdaDtl {

    @EmbeddedId
    private PdaFdaDtlId id;

    @Column(name = "CHARGE_POID", nullable = false)
    private Long chargePoid;

    @Column(name = "CURRENCY_CODE", length = 20)
    private String currencyCode;

    @Column(name = "CURRENCY_RATE")
    private BigDecimal currencyRate;

    @Column(name = "DETAILS_FROM", length = 30)
    private String detailsFrom;

    @Column(name = "QTY")
    private BigDecimal qty;

    @Column(name = "DAYS")
    private BigDecimal days;

    @Column(name = "PDA_RATE")
    private BigDecimal pdaRate;

    @Column(name = "RATE_TYPE_POID")
    private Long rateTypePoid;

    @Column(name = "MANUAL", length = 1)
    private String manual;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "REMARKS", length = 1000)
    private String remarks;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "REMARK_QTY_DAYS", length = 100)
    private String remarkQtyDays;

    @Column(name = "COST_AMOUNT")
    private BigDecimal costAmount;

    @Column(name = "FDA_AMOUNT")
    private BigDecimal fdaAmount;

    @Column(name = "SEQNO")
    private Integer seqNo;

    @Column(name = "PRINCIPAL_POID")
    private Long principalPoid;

    @Column(name = "REF_DET_ROW_ID")
    private Long refDetRowId;

    @Column(name = "REF_DOC_ID", length = 20)
    private String refDocId;

    @Column(name = "REF_DOC_POID")
    private Long refDocPoid;

    @Column(name = "BOOKED_DOC_POID", length = 20)
    private String bookedDocPoid;

    @Column(name = "DN_DOC_ID", length = 20)
    private String dnDocId;

    @Column(name = "DN_DOC_POID", length = 20)
    private String dnDocPoid;

    @Column(name = "FDA_REMARKS", length = 500)
    private String fdaRemarks;

    @Column(name = "DN_FROM", length = 30)
    private String dnFrom;

    @Column(name = "CN_DOC_ID", length = 20)
    private String cnDocId;

    @Column(name = "CN_DOC_POID", length = 20)
    private String cnDocPoid;

    @Column(name = "DN_AMOUNT")
    private BigDecimal dnAmount;

    @Column(name = "CN_AMOUNT")
    private BigDecimal cnAmount;

    @Column(name = "CN_DET_ROW_ID", length = 20)
    private String cnDetRowId;

    @Column(name = "DN_DET_ROW_ID", length = 20)
    private String dnDetRowId;

    @Column(name = "TAX_POID")
    private Long taxPoid;

    @Column(name = "TAX_PERCENTAGE")
    private BigDecimal taxPercentage;

    @Column(name = "TAX_AMOUNT")
    private BigDecimal taxAmount;

    @Column(name = "DN_TAX_POID")
    private Long dnTaxPoid;

    @Column(name = "DN_TAX_PERCENTAGE")
    private BigDecimal dnTaxPercentage;

    @Column(name = "DN_TAX_AMOUNT")
    private BigDecimal dnTaxAmount;

    @Column(name = "DN_TOTAL_AMOUNT")
    private BigDecimal dnTotalAmount;

    @Column(name = "CN_TAX_POID")
    private Long cnTaxPoid;

    @Column(name = "CN_TAX_PERCENTAGE")
    private BigDecimal cnTaxPercentage;

    @Column(name = "CN_TAX_AMOUNT")
    private BigDecimal cnTaxAmount;

    @Column(name = "CN_TOTAL_AMOUNT")
    private BigDecimal cnTotalAmount;

    @Column(name = "PDA_POID")
    private Long pdaPoid;

    @Column(name = "PDA_DET_ROW_ID")
    private Long pdaDetRowId;

    @Column(name = "PRINT_SEQ_NO")
    private Long printSeqNo;
}
