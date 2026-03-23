package com.asg.shipping.importmanifestupdate.entity;


import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "SHIP_BL_MANIFEST_GENERAL_DTL")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipBlManifestGeneralDtl {

    @EmbeddedId
    private ShipBlManifestDtlId id;

    @Column(name = "COMODITY_POID")
    private Long comodityPoid;

    @Column(name = "CARGO_DESCRIPTION", length = 200)
    private String cargoDescription;

    @Column(name = "QUANTITY")
    private Long quantity;

    @Column(name = "GRS_VOLUME")
    private Long grsVolume;

    @Column(name = "GRS_WEIGHT")
    private Long grsWeight;

    @Column(name = "NET_VOLUME")
    private Long netVolume;

    @Column(name = "NET_WEIGHT")
    private Long netWeight;

    @Column(name = "TARE_WEIGHT")
    private Long tareWeight;

    @Column(name = "NO_OF_PACKS")
    private Long noOfPacks;

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
}
