package com.asg.shipping.exportManifestUpdate.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity class for SHIP_BL_MANIFEST_CARGO_DTL table
 * Used for both Description (DESCRIPTION_TYPE='CARGO') and Marks (DESCRIPTION_TYPE='MARKS')
 */
@Entity
@Table(name = "SHIP_BL_MANIFEST_CARGO_DTL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ExportShipBlManifestCargoDtlId.class)
public class ExportShipBlManifestCargoDtl {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Id
    @Column(name = "DESCRIPTION_TYPE", nullable = false, length = 10)
    private String descriptionType;

    @Column(name = "CARGO_DESCRIPTION", length = 1000)
    private String cargoDescription;

    @Column(name = "RECORD_ORDER", precision = 10, scale = 0)
    private Long recordOrder;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (descriptionType == null) {
            descriptionType = "CARGO";
        }
        // Trigger logic: Uppercase cargo description
        if (cargoDescription != null) {
            cargoDescription = cargoDescription.toUpperCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = LocalDateTime.now();
        // Trigger logic: Uppercase cargo description
        if (cargoDescription != null) {
            cargoDescription = cargoDescription.toUpperCase();
        }
    }
}

