package com.asg.shipping.collectionhandover.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity class for AR_SH_DAY_END_CLOSE_DTL table
 */
@Entity(name = "CollectionHandoverDtl")
@Table(name = "AR_SH_DAY_END_CLOSE_DTL")
@IdClass(ArShDayEndCloseDtlId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArShDayEndCloseDtl {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "CURRENCY_AMOUNT", precision = 18, scale = 2)
    private BigDecimal currencyAmount;

    @Column(name = "CURRENCY_TYPE", length = 20)
    private String currencyType;

    @Column(name = "NO_OF_TRAN")
    private Integer noOfTran;

    @Column(name = "CASH_AMOUNT", precision = 18, scale = 2)
    private BigDecimal cashAmount;

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
    private ArShDayEndCloseHdr header;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = LocalDateTime.now();
    }
}

