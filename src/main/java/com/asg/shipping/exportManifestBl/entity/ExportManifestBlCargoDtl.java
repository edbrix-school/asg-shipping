package com.asg.shipping.exportManifestBl.entity;

import com.asg.common.lib.entity.BaseEntity;
import com.asg.shipping.importmanifestupdate.entity.ShipBlManifestCargoDtlId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "SHIP_BL_MANIFEST_CARGO_DTL")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportManifestBlCargoDtl extends BaseEntity {

    @EmbeddedId
    private ShipBlManifestCargoDtlId id;

    @Column(name = "CARGO_DESCRIPTION", length = 1000)
    private String cargoDescription;

    @Column(name = "RECORD_ORDER", precision = 10, scale = 0)
    private Long recordOrder;

}

