package com.asg.shipping.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "SHIP_LINE_MASTER_TYPE_DTL")
@IdClass(ShipLineMasterTypeId.class)
@Getter
@Setter
public class ShipLineMasterType {

    @Id
    @Column(name = "LINE_POID", nullable = false)
    private Long linePoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "CONTAINER_TYPE_POID")
    private Long containerTypePoid;

    @Column(name = "VALID_UNTIL")
    private LocalDate validUntil;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;
}


