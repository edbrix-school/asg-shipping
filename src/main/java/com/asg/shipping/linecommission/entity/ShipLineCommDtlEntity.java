package com.asg.shipping.linecommission.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "SHIP_LINE_COMM_DTL")
@IdClass(ShipLineCommDtlId.class)
@Getter
@Setter
public class ShipLineCommDtlEntity extends BaseEntity {

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
    private BigDecimal amount;

    @Column(name = "PERCENT")
    private BigDecimal percent;

    @Column(name = "PAYBACK_PERCENT")
    private BigDecimal paybackPercent;

    @Column(name = "OUR_BOOKING_PERCENTAGE")
    private BigDecimal ourBookingPercentage;

    @Column(name = "DEST_LOAD_PERCENTAGE")
    private BigDecimal destLoadPercentage;

    @Column(name = "SHORT_LEG_PERCENTAGE")
    private BigDecimal shortLegPercentage;

    @Column(name = "SPL_EQP_PERCENTAGE")
    private BigDecimal splEqpPercentage;

    @Column(name = "AMOUNT_PER_TUE")
    private BigDecimal amountPerTue;

    @Column(name = "PP_BOOKING_PERCENTAGE_COLLECT")
    private BigDecimal ppBookingPercentageCollect;

    @Column(name = "MIN_AMOUNT_LESSER")
    private BigDecimal minAmountLesser;

    @Column(name = "MIN_COMM_AMOUNT")
    private BigDecimal minCommAmount;

    @Column(name = "REMARKS")
    private String remarks;

}


