package com.asg.shipping.importManifestUpdate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ShipBlManifestCargoDtlId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "DESCRIPTION_TYPE", nullable = false, length = 10)
    private String descriptionType;
}
