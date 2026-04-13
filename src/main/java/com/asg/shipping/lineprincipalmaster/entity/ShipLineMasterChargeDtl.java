package com.asg.shipping.lineprincipalmaster.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Entity class for SHIP_LINE_MASTER_CHARGE_DTL table
 */
@Entity
@Table(name = "SHIP_LINE_MASTER_CHARGE_DTL",
       uniqueConstraints = {
           @UniqueConstraint(name = "SHIP_LINE_MASTER_CHARGE_DTL_UK", columnNames = {"LINE_POID", "CHARGE_POID"})
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ShipLineMasterChargeDtlId.class)
public class ShipLineMasterChargeDtl extends BaseEntity {

    @Id
    @Column(name = "LINE_POID", nullable = false)
    private Long linePoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "CHARGE_POID")
    private Long chargePoid;

    @Column(name = "LINE_CHARGE_CODE", length = 25)
    private String lineChargeCode;

    @Column(name = "LINE_CHARGE_DESCRIPTION", length = 50)
    private String lineChargeDescription;

    @Column(name = "VALID_UNTIL")
    private LocalDate validUntil;

    @Column(name = "REMUN_COMMISSION_CHARGE", length = 1)
    private String remunCommissionCharge;

    @Column(name = "EXCLUDED_FROM_EDI", length = 25)
    private String excludedFromEdi;

    @Column(name = "DEFAULT_PRINT_GROUP_EDI", length = 50)
    private String defaultPrintGroupEdi;

    @Column(name = "WKYRPT_INCLUDE_AS")
    private Long wkyrptIncludeAs;

    @AuditIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LINE_POID", insertable = false, updatable = false)
    private ShipLineMaster lineMaster;

   /* @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = LocalDateTime.now();
    }*/
}

