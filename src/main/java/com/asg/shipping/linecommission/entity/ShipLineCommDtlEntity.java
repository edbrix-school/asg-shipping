package com.asg.shipping.linecommission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "SHIP_LINE_COMM_DTL")
@IdClass(ShipLineCommDtlId.class)
@Getter
@Setter
public class ShipLineCommDtlEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "REMUNERATION_POID")
    private Long remunerationPoid;

    @Column(name = "CURRENCY_POID")
    private Long currencyPoid;

    @Column(name = "AMOUNT")
    private Long amount;

    @Column(name = "PERCENT")
    private Long percent;

    @Column(name = "PAYBACK_PERCENT")
    private Long paybackPercent;

    @Column(name = "OUR_BOOKING_PERCENTAGE")
    private Long ourBookingPercentage;

    @Column(name = "DEST_LOAD_PERCENTAGE")
    private Long destLoadPercentage;

    @Column(name = "SHORT_LEG_PERCENTAGE")
    private Long shortLegPercentage;

    @Column(name = "SPL_EQP_PERCENTAGE")
    private Long splEqpPercentage;

    @Column(name = "AMOUNT_PER_TUE")
    private Long amountPerTue;

    @Column(name = "PP_BOOKING_PERCENTAGE_COLLECT")
    private Long ppBookingPercentageCollect;

    @Column(name = "MIN_AMOUNT_LESSER")
    private Long minAmountLesser;

    @Column(name = "MIN_COMM_AMOUNT")
    private Long minCommAmount;

    @Column(name = "REMARKS")
    private String remarks;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;
}


