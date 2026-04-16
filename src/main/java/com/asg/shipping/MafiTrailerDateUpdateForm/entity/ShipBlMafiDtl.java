package com.asg.shipping.mafitrailerdateupdateform.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "SHIP_BL_MAFI_DTL")
@IdClass(ShipBlMafiDtl.CompositeKey.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShipBlMafiDtl extends BaseEntity {

    @Id
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID")
    private Long detRowId;

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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompositeKey implements Serializable {
        private Long transactionPoid;
        private Long detRowId;
    }
}
