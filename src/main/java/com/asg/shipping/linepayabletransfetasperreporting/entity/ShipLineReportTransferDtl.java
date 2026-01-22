package com.asg.shipping.linepayabletransfetasperreporting.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.asg.common.lib.security.util.UserContext.getUserName;
/**
 * Entity class for SHIP_LINE_REPORT_TRANSFER_DTL table
 */
@Entity
@Table(name = "SHIP_LINE_REPORT_TRANSFER_DTL")
@IdClass(ShipLineReportTransferDtlId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class ShipLineReportTransferDtl {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "MAINFEST_TRANSACTION_POID")
    private Long mainfestTransactionPoid;

    @Column(name = "BL_NUMBER", length = 50)
    private String blNumber;

    @Column(name = "ACUTAL_AMOUNT", precision = 20, scale = 3)
    private BigDecimal acutalAmount;

    @Column(name = "TOTAL_AMOUNT_TRANSFER", precision = 20, scale = 3)
    private BigDecimal totalAmountTransfer;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "IS_SELECT", length = 25)
    @Builder.Default
    private String isSelect = "N";

    @Column(name = "CHARGE_POID")
    private Long chargePoid;

    @Column(name = "FREIGHT_TYPE", length = 25)
    private String freightType;

    @Column(name = "CURRENCY_CODE", length = 25)
    private String currencyCode;

    @Column(name = "CURRENCY_EXCHANGE")
    private BigDecimal currencyExchange;

    @Column(name = "CURRENCY_AMOUNT")
    private BigDecimal currencyAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID", insertable = false, updatable = false)
    private ShipLineReportTransferHdr header;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (createdBy == null) {
            createdBy = getUserName();
        }
        if (isSelect == null) {
            isSelect = "N";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = LocalDateTime.now();
        if (lastModifiedBy == null) {
            lastModifiedBy = getUserName();
        }
    }
}
