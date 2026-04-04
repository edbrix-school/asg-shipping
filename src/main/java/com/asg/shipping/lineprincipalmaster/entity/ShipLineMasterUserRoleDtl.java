package com.asg.shipping.lineprincipalmaster.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "SHIP_LINE_MASTER_USER_ROLE_DTL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ShipLineMasterUserRoleDtlId.class)
public class ShipLineMasterUserRoleDtl extends BaseEntity {

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

}
