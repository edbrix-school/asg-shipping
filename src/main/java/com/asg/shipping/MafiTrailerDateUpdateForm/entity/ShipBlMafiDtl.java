package com.asg.shipping.mafitrailerdateupdateform.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "SHIP_BL_MAFI_DTL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShipBlMafiDtl extends BaseEntity {

    @EmbeddedId
    private ShipBlMafiDtlId id;

    @Column(name = "BL_POID")
    private Long blPoid;

    @Column(name = "MAFI_REF", length = 100)
    private String mafiRef;

    @Column(name = "MAFI_SIZE")
    private BigDecimal mafiSize;

    @Column(name = "MAFI_FREE_DAYS")
    private BigDecimal mafiFreeDays;

    @Column(name = "BACK_LOAD_DATE")
    private LocalDate backLoadDate;

    @Column(name = "MAFI_EMPTY_DATE")
    private LocalDate mafiEmptyDate;

    @Column(name = "REMARKS", length = 200)
    private String remarks;
}
