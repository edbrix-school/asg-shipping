package com.asg.shipping.importmanifestupdate.entity;


import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDate;

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
    private LocalDate mafiEmptyDate;

    @Column(name = "BACK_LOAD_DATE")
    private LocalDate backLoadDate;

}
