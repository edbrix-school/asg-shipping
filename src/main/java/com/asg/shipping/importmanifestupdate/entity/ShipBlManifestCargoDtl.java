package com.asg.shipping.importmanifestupdate.entity;


import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "SHIP_BL_MANIFEST_CARGO_DTL")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipBlManifestCargoDtl {

    @EmbeddedId
    private ShipBlManifestCargoDtlId id;

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
}
