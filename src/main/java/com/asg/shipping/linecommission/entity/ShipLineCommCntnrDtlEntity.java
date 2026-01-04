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
@Table(name = "SHIP_LINE_COMM_CNTNR_DTL")
@IdClass(ShipLineCommCntnrDtlId.class)
@Getter
@Setter
public class ShipLineCommCntnrDtlEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "CONTAINER_TYPE_POID")
    private Long containerTypePoid;

    @Column(name = "IMPORT_BOX_RATE")
    private Long importBoxRate;

    @Column(name = "EXPORT_BOX_RATE")
    private Long exportBoxRate;

    @Column(name = "TRANSHIP_BOX_RATE")
    private Long transhipBoxRate;

    @Column(name = "SHORT_LEG_AMOUNT")
    private Long shortLegAmount;

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


