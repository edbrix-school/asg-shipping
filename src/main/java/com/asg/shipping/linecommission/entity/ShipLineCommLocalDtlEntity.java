package com.asg.shipping.linecommission.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "SHIP_LINE_COMM_LOCAL_DTL")
@IdClass(ShipLineCommLocalDtlId.class)
@Getter
@Setter
public class ShipLineCommLocalDtlEntity extends BaseEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "CHARGE_POID")
    private Long chargePoid;

    @Column(name = "CURRENCY_POID")
    private Long currencyPoid;

    @Column(name = "PERCENT")
    private Long percent;

    @Column(name = "SHARE_AMOUNT")
    private Long shareAmount;

    @Column(name = "REMARKS")
    private String remarks;
}


