package com.asg.shipping.exportManifestUpdate.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity class for SHIP_BL_MANIFEST_GENERAL_DTL table
 */
@Entity
@Table(name = "SHIP_BL_MANIFEST_GENERAL_DTL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ExportShipBlManifestGeneralDtlId.class)
public class ExportShipBlManifestGeneralDtl {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "COMODITY_POID")
    private Long comodityPoid;

    @Column(name = "CARGO_DESCRIPTION", length = 200)
    private String cargoDescription;

    @Column(name = "QUANTITY", precision = 25, scale = 3)
    private BigDecimal quantity;

    @Column(name = "GRS_VOLUME", precision = 25, scale = 3)
    private BigDecimal grsVolume;

    @Column(name = "GRS_WEIGHT", precision = 25, scale = 3)
    private BigDecimal grsWeight;

    @Column(name = "NET_VOLUME", precision = 25, scale = 3)
    private BigDecimal netVolume;

    @Column(name = "NET_WEIGHT", precision = 25, scale = 3)
    private BigDecimal netWeight;

    @Column(name = "TARE_WEIGHT", precision = 25, scale = 3)
    private BigDecimal tareWeight;

    @Column(name = "NO_OF_PACKS", precision = 25, scale = 3)
    private BigDecimal noOfPacks;

    @Column(name = "PACK_UNIT", length = 6)
    private String packUnit;

    @Column(name = "DESTINATION_PORT_POID")
    private Long destinationPortPoid;

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
        // Trigger logic: If NET_VOLUME is null and GRS_VOLUME is not null, set NET_VOLUME = GRS_VOLUME
        if (netVolume == null && grsVolume != null) {
            netVolume = grsVolume;
        }
        // Trigger logic: If NO_OF_PACKS is null and QUANTITY is not null, set NO_OF_PACKS = QUANTITY
        if (noOfPacks == null && quantity != null) {
            noOfPacks = quantity;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = LocalDateTime.now();
        // Trigger logic: If NET_VOLUME is null and GRS_VOLUME is not null, set NET_VOLUME = GRS_VOLUME
        if (netVolume == null && grsVolume != null) {
            netVolume = grsVolume;
        }
        // Trigger logic: If NO_OF_PACKS is null and QUANTITY is not null, set NO_OF_PACKS = QUANTITY
        if (noOfPacks == null && quantity != null) {
            noOfPacks = quantity;
        }
    }
}

