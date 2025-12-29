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
@Table(name = "SHIP_LINE_COMM_LOCAL_DTL")
@IdClass(ShipLineCommLocalDtlId.class)
@Getter
@Setter
public class ShipLineCommLocalDtlEntity {

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

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;
}


