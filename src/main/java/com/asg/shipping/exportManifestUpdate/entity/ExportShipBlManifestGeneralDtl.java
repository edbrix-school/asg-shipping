package com.asg.shipping.exportManifestUpdate.entity;

import com.asg.common.lib.entity.BaseEntity;
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
public class ExportShipBlManifestGeneralDtl extends BaseEntity {

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

}

