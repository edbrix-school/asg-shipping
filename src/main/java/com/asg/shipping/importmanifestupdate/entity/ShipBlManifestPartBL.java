package com.asg.shipping.importmanifestupdate.entity;


import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "SHIP_BL_MANIFEST_CONTAINER_PRT")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipBlManifestPartBL {

    @EmbeddedId
    private ShipBlManifestDtlId id;

    @Column(name = "SHIPPER_NAME", length = 200)
    private String shipperName;

    @Column(name = "CONSIGNEE_NAME", length = 200)
    private String consigneeName;

    @Column(name = "CONTAINER_NO", length = 25)
    private String containerNo;

    @Column(name = "CARGO_DESCRIPTION", length = 200)
    private String cargoDescription;

    @Column(name = "COMODITY_POID")
    private Long comodityPoid;

    @Column(name = "NET_VOLUME")
    private Long netVolume;

    @Column(name = "NET_WEIGHT")
    private Long netWeight;

    @Column(name = "NO_OF_PACKS")
    private Long noOfPacks;

    @Column(name = "PACK_UNIT", length = 6)
    private String packUnit;

    @Column(name = "PART_BL_NUMBER", length = 25)
    private String partBlNumber;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;
}
