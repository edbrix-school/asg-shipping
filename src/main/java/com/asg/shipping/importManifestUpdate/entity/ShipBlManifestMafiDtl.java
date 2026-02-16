package com.asg.shipping.importManifestUpdate.entity;


import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "SHIP_BL_MANIFEST_MAFI_DTL")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipBlManifestMafiDtl {

    @EmbeddedId
    private ShipBlManifestDtlId id;

    @Column(name = "MAFI_REF", length = 100)
    private String mafiRef;

    @Column(name = "REMARKS", length = 200)
    private String remarks;

    @Column(name = "MAFI_FREE_DAYS")
    private Long mafiFreeDays;

    @Column(name = "MAFI_SIZE")
    private Long mafiSize;

    @Column(name = "MAFI_EMPTY_DATE")
    private LocalDateTime mafiEmptyDate;

    @Column(name = "BACK_LOAD_DATE")
    private LocalDateTime backLoadDate;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;
}
