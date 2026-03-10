package com.asg.shipping.shippingmanifestcorrector.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import static com.asg.common.lib.security.util.UserContext.*;

/**
 * Entity class for SHIP_BL_REPRINT_CONTAINER_DTL table
 */
@Entity
@Table(name = "SHIP_BL_REPRINT_CONTAINER_DTL")
@IdClass(ShipBlReprintContainerDtlId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class ShipBlReprintContainerDtl extends BaseEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "CONTAINER_NUMBER", length = 50)
    private String containerNumber;

    @Column(name = "EQUIPMENT_ISO_TYPE", length = 50)
    private String equipmentIsoType;

    @Column(name = "IS_SELECTED_DLV", length = 10)
    private String isSelectedDlv;

    @Column(name = "IS_SELECTED_RTN", length = 10)
    private String isSelectedRtn;


}

