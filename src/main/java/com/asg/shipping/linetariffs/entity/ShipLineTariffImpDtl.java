package com.asg.shipping.linetariffs.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity class for SHIP_LINE_TARIFF_IMP_DTL table (Import Demurrage Collectable)
 */
@Entity
@Table(name = "SHIP_LINE_TARIFF_IMP_DTL")
@IdClass(ShipLineTariffImpDtlId.class)
@Data
@NoArgsConstructor
public class ShipLineTariffImpDtl {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "CONTAINER_TYPE_POID")
    private Long containerTypePoid;

    @Column(name = "FREE_DAYS")
    private Integer freeDays;

    @Column(name = "SLAB1_TILLDAYS")
    private Integer slab1Tilldays;

    @Column(name = "SLAB1_RATE")
    private BigDecimal slab1Rate;

    @Column(name = "SLAB2_TILLDAYS")
    private Integer slab2Tilldays;

    @Column(name = "SLAB2_RATE")
    private BigDecimal slab2Rate;

    @Column(name = "SLAB3_TILLDAYS")
    private Integer slab3Tilldays;

    @Column(name = "SLAB3_RATE")
    private BigDecimal slab3Rate;

    @Column(name = "SLAB4_TILLDAYS")
    private Integer slab4Tilldays;

    @Column(name = "SLAB4_RATE")
    private BigDecimal slab4Rate;

    @Column(name = "SLAB5_TILLDAYS")
    private Integer slab5Tilldays;

    @Column(name = "SLAB5_RATE")
    private BigDecimal slab5Rate;

    @Column(name = "SLAB6_TILLDAYS")
    private Integer slab6Tilldays;

    @Column(name = "SLAB6_RATE")
    private BigDecimal slab6Rate;

    @Column(name = "SLAB7_TILLDAYS")
    private Integer slab7Tilldays;

    @Column(name = "SLAB7_RATE")
    private BigDecimal slab7Rate;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID", insertable = false, updatable = false)
    private ShipLineTariffHdr tariffHdr;

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

