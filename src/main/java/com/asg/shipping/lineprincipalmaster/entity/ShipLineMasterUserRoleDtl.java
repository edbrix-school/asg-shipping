package com.asg.shipping.lineprincipalmaster.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "SHIP_LINE_MASTER_USER_ROLE_DTL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ShipLineMasterUserRoleDtlId.class)
public class ShipLineMasterUserRoleDtl {

    @Id
    @Column(name = "LINE_POID", nullable = false)
    private Long linePoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "USER_ROLE_POID")
    private Long userRolePoid;

    @Column(name = "VALID_UNTIL")
    private LocalDate validUntil;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;
}
