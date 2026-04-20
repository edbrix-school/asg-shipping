package com.asg.shipping.portstoragetariffsmaster.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Entity class for SHIP_PORT_TARIFF_DTL table
 */
@Entity
@Table(name = "SHIP_PORT_TARIFF_DTL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ShipPortTariffDtlId.class)
public class ShipPortTariffDtl extends BaseEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "CONTAINER_TYPE_POID")
    private Long containerTypePoid;

    @Column(name = "CONTAINER_SIZE", precision = 10, scale = 3)
    private BigDecimal containerSize;

    @Column(name = "FREE_DAYS")
    private Integer freeDays;

    @Column(name = "SLAB1_TILLDAYS")
    private Integer slab1Tilldays;

    @Column(name = "SLAB1_RATE", precision = 18, scale = 2)
    private BigDecimal slab1Rate;

    @Column(name = "SLAB2_TILLDAYS")
    private Integer slab2Tilldays;

    @Column(name = "SLAB2_RATE", precision = 18, scale = 2)
    private BigDecimal slab2Rate;

    @Column(name = "SLAB3_TILLDAYS")
    private Integer slab3Tilldays;

    @Column(name = "SLAB3_RATE", precision = 18, scale = 2)
    private BigDecimal slab3Rate;

    @Column(name = "SLAB4_TILLDAYS")
    private Integer slab4Tilldays;

    @Column(name = "SLAB4_RATE", precision = 18, scale = 2)
    private BigDecimal slab4Rate;

    @Column(name = "SLAB5_TILLDAYS")
    private Integer slab5Tilldays;

    @Column(name = "SLAB5_RATE", precision = 18, scale = 2)
    private BigDecimal slab5Rate;

    @Column(name = "SLAB6_TILLDAYS")
    private Integer slab6Tilldays;

    @Column(name = "SLAB6_RATE", precision = 18, scale = 2)
    private BigDecimal slab6Rate;

    @Column(name = "SLAB7_TILLDAYS")
    private Integer slab7Tilldays;

    @Column(name = "SLAB7_RATE", precision = 18, scale = 2)
    private BigDecimal slab7Rate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID", insertable = false, updatable = false)
    private ShipPortTariffHdr tariffHdr;

}