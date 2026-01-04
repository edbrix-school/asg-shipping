package com.asg.shipping.vesseltypemaster.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity class for SHIP_VESSEL_TYPE_MASTER table
 */
@Entity
@Table(name = "SHIP_VESSEL_TYPE_MASTER")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipVesselTypeMaster {

    @Id
    @Column(name = "VESSEL_TYPE_POID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long vesselTypePoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "VESSEL_TYPE_CODE", nullable = false, length = 20, unique = true, updatable = false)
    private String vesselTypeCode;

    @Column(name = "VESSEL_TYPE_NAME", nullable = false, length = 100, unique = true)
    private String vesselTypeName;

    @Column(name = "VESSEL_TYPE_NAME2", length = 100)
    private String vesselTypeName2;

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

    @Column(name = "COST_CENTER_POID")
    private Long costCentrePoid;


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
