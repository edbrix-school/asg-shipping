package com.asg.shipping.demurragedetentionpayabletransfer.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity class for SHIP_DEM_DETN_TRANSFER_DTL table
 */
@Entity
@Table(name = "SHIP_DEM_DETN_TRANSFER_DTL")
@IdClass(ShipDemDetnTransferDtlId.class)
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ShipDemDetnTransferDtl extends BaseEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "MAINFEST_TRANSACTION_POID")
    private Long mainfestTransactionPoid;

    @Column(name = "LINE_POID")
    private Long linePoid;

    @Column(name = "JOB_NO", length = 20)
    private String jobNo;

    @Column(name = "CONSIGNEE", length = 500)
    private String consignee;

    @Column(name = "NOTIFY", length = 500)
    private String notify;

    @Column(name = "BL_NUMBER", length = 50)
    private String blNumber;

    @Column(name = "CONTAINER_NO", length = 25)
    private String containerNo;

    @Column(name = "SAIL_DATE")
    private LocalDate sailDate;

    @Column(name = "ARRIVAL_DATE")
    private LocalDate arrivalDate;

    @Column(name = "EMPTY_IN")
    private LocalDate emptyIn;

    @Column(name = "EQUIPMENT_ISO_TYPE", length = 20)
    private String equipmentIsoType;

    @Column(name = "DEMURRAGE_ACUTAL", precision = 20, scale = 3)
    private BigDecimal demurrageAcutal;

    @Column(name = "EXTRA_FREE_DAYS", precision = 20, scale = 3)
    private BigDecimal extraFreeDays;

    @Column(name = "EXTRA_FREE_DAYS_PRNPLS", precision = 20, scale = 3)
    private BigDecimal extraFreeDaysPrnpls;

    @Column(name = "START_DATE")
    private LocalDate startDate;

    @Column(name = "END_DATE")
    private LocalDate endDate;

    @Column(name = "TOTAL_COLLECTED_DAYS", precision = 20, scale = 3)
    private BigDecimal totalCollectedDays;

    @Column(name = "TOTAL_COLLECTED_AMT", precision = 20, scale = 3)
    private BigDecimal totalCollectedAmt;

    @Column(name = "TOTAL_SHORT_EXCESS_AMOUNT", precision = 20, scale = 3)
    private BigDecimal totalShortExcessAmount;

    @Column(name = "TOTAL_PAYABLE_AMOUNT", precision = 20, scale = 3)
    private BigDecimal totalPayableAmount;

    @Column(name = "TOTAL_INCOME_AMOUNT", precision = 20, scale = 3)
    private BigDecimal totalIncomeAmount;

    @Column(name = "NET_INCOME_AMT", precision = 20, scale = 3)
    private BigDecimal netIncomeAmt;

    @Column(name = "IS_SELECT", length = 25)
    private String isSelect;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID", insertable = false, updatable = false)
    @com.asg.common.lib.annotation.AuditIgnore
    private ShipDemDetnTransferHdr shipDemDetnTransferHdr;

    @PrePersist
    protected void onCreate() {
        if (isSelect == null) {
            isSelect = "N";
        }
    }
}
