package com.asg.shipping.exportManifestBl.entity;

import com.asg.common.lib.entity.BaseEntity;
import com.asg.shipping.importmanifestupdate.entity.ShipBlManifestDtlId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "SHIP_BL_MANIFEST_GENERAL_DTL")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportManifestBlGeneralDtl extends BaseEntity {

    @EmbeddedId
    private ShipBlManifestDtlId id;

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
