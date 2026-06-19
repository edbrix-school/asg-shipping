package com.asg.shipping.linecommission.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "SHIP_LINE_COMM_CNTNR_DTL")
@IdClass(ShipLineCommCntnrDtlId.class)
@Getter
@Setter
public class ShipLineCommCntnrDtlEntity extends BaseEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "CONTAINER_TYPE_POID")
    private Long containerTypePoid;

    @Column(name = "IMPORT_BOX_RATE")
    private BigDecimal importBoxRate;

    @Column(name = "EXPORT_BOX_RATE")
    private BigDecimal exportBoxRate;

    @Column(name = "TRANSHIP_BOX_RATE")
    private BigDecimal transhipBoxRate;

    @Column(name = "SHORT_LEG_AMOUNT")
    private BigDecimal shortLegAmount;

    @Column(name = "REMARKS")
    private String remarks;

}


