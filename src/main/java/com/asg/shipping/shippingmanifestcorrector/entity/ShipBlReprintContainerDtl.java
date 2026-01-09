package com.asg.shipping.shippingmanifestcorrector.entity;

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
public class ShipBlReprintContainerDtl {

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

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (createdBy == null) {
            createdBy = getUserName();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = LocalDateTime.now();
        if (lastModifiedBy == null) {
            lastModifiedBy = getUserName();
        }
    }
}

