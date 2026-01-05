package com.asg.shipping.lineprincipalmaster.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
public class ShipLineMasterChargeDtl {

    @Id
    @Column(name = "LINE_POID", nullable = false)
    private Long linePoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "charge_dtl_seq")
    @SequenceGenerator(name = "charge_dtl_seq", sequenceName = "SHIP_LINE_MASTER_CHARGE_DTL_SEQ", allocationSize = 1)
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

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LINE_POID", insertable = false, updatable = false)
    private ShipLineMaster lineMaster;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = LocalDateTime.now();
    }
}

