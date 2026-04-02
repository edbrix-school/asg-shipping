package com.asg.shipping.lineprincipalmaster.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "SHIP_LINE_MASTER_PIC_DTL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ShipLineMasterPicDtlId.class)
public class ShipLineMasterPicDtl {

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

    @Column(name = "CREATED_BY", length = 30)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 30)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;
}
