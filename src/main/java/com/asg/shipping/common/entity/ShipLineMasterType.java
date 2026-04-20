package com.asg.shipping.common.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "SHIP_LINE_MASTER_TYPE_DTL")
@IdClass(ShipLineMasterTypeId.class)
@Getter
@Setter
public class ShipLineMasterType extends BaseEntity {

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

}


