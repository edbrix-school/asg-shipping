package com.asg.shipping.bookingFormSH.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;


@Entity
@Table(name = "SHIP_MATE_STUFFING_DTL")
@IdClass(ShipMateStuffingDtlld.class)
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ShipMateStuffingDtl extends BaseEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "CONTAINER_NO", length = 25)
    private String containerNo;

    @Column(name = "EQUIPMENT_SEAL_NO", length = 25)
    private String equipmentSealNo;

    @Column(name = "EQUIPMENT_ISO_TYPE", length = 20)
    private String equipmentIsoType;

    @Column(name = "MARKS", length = 200)
    private String marks;

    @Column(name = "COLOUR_CODE", length = 200)
    private String colourCode;

    @Column(name = "WEIGHT_TONNES", precision = 20, scale = 3)
    private BigDecimal weightTonnes;

    @Column(name = "QTY_OF_BUNDLES", precision = 20, scale = 3)
    private BigDecimal qtyOfBundles;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID", insertable = false, updatable = false)
    private ShipMateHdr shipMateHdr;

}
