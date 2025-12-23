package com.asg.shipping.groupcontainertypes.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity class for SHIP_CONTAINER_TYPE_GRP_MASTER table
 */
@Entity
@Table(name = "SHIP_CONTAINER_TYPE_GRP_MASTER")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipContainerTypeGrpMaster {

    @Id
    @Column(name = "CONTAINER_GRP_POID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long containerGrpPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "CONTAINER_GRP_CODE", nullable = false, length = 20)
    private String containerGrpCode;

    @Column(name = "CONTAINER_GRP_NAME", nullable = false, length = 100)
    private String containerGrpName;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "SEQNO")
    private Integer seqno;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (deleted == null) {
            deleted = "N";
        }
        if (active == null) {
            active = "Y";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = LocalDateTime.now();
    }
}

