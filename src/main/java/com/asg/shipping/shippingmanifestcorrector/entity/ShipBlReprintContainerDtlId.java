package com.asg.shipping.shippingmanifestcorrector.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key for SHIP_BL_REPRINT_CONTAINER_DTL
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipBlReprintContainerDtlId implements Serializable {
    private Long transactionPoid;
    private Long detRowId;
}

