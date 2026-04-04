package com.asg.shipping.lineprincipalmaster.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "SHIP_LINE_MASTER_PIC_DTL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ShipLineMasterPicDtlId.class)
public class ShipLineMasterPicDtl extends BaseEntity {

    @Id
    @Column(name = "LINE_POID", nullable = false)
    private Long linePoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "DEPARTMENT_POID")
    private Long departmentPoid;

    @Column(name = "HANDLED_USER_POID")
    private Long handledUserPoid;

    @Column(name = "PERIOD_FROM")
    private LocalDate periodFrom;

    @Column(name = "PERIOD_TO")
    private LocalDate periodTo;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

}
