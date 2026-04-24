package com.asg.shipping.collectionhandover.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Entity for AR_SH_DAY_END_CLOSE_DTL.
 *
 * PK: (TRANSACTION_POID, DET_ROW_ID) — composite, no sequence.
 * CURRENCY_AMOUNT / CASH_AMOUNT — plain NUMBER in DB, no precision/scale.
 * NO_OF_TRAN — plain NUMBER, mapped as Integer.
 * Audit columns — LASTMODIFIED_BY / LASTMODIFIED_DATE (no extra _).
 */
@Entity(name = "CollectionHandoverDtl")
@Table(name = "AR_SH_DAY_END_CLOSE_DTL")
@IdClass(ArShDayEndCloseDtlId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArShDayEndCloseDtl extends BaseEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "CURRENCY_AMOUNT")
    private BigDecimal currencyAmount;

    @Column(name = "CURRENCY_TYPE", length = 20)
    private String currencyType;

    @Column(name = "NO_OF_TRAN")
    private Integer noOfTran;

    @Column(name = "CASH_AMOUNT")
    private BigDecimal cashAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID", insertable = false, updatable = false)
    private ArShDayEndCloseHdr header;
}

