package com.asg.shipping.containertypes.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity class for SHIP_CONTAINER_TYPE_MASTER table
 */
@Entity
@Table(name = "SHIP_CONTAINER_TYPE_MASTER")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipContainerTypeMaster extends BaseEntity {

    @Id
    @Column(name = "CONTAINER_TYPE_POID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long containerTypePoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "CONTAINER_TYPE_CODE", nullable = false, length = 20)
    private String containerTypeCode;

    @Column(name = "CONTAINER_TYPE_NAME", nullable = false, length = 100)
    private String containerTypeName;

    @Column(name = "CONTAINER_TYPE_SIZE", nullable = false, length = 20)
    private String containerTypeSize;

    @Column(name = "CONTAINER_TYPE_ISO_NAME", nullable = false, length = 100)
    private String containerTypeIsoName;

    @Column(name = "CONTAINER_CARGO_WEIGHT", precision = 25, scale = 3)
    private BigDecimal containerCargoWeight;

    @Column(name = "CONTAINER_TARE_WEIGHT", precision = 25, scale = 3)
    private BigDecimal containerTareWeight;

    @Column(name = "CONTAINER_TEU_FACTOR", nullable = false, precision = 25, scale = 3)
    private BigDecimal containerTeuFactor;

    @Column(name = "CONTAINER_TYPE_CATEGORY", nullable = false, length = 20)
    private String containerTypeCategory;

    @Column(name = "CONTAINER_GRP_POID")
    private Long containerGrpPoid;

    @Column(name = "CONTAINER_APMT_TYPE_CODE", length = 10)
    private String containerApmtTypeCode;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "SEQNO")
    private Integer seqno;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @PrePersist
    protected void onCreate() {
        if (deleted == null) {
            deleted = "N";
        }
        if (active == null) {
            active = "Y";
        }
    }

  /*  @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = LocalDateTime.now();
    }*/
}


