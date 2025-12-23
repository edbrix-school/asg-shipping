package com.asg.shipping.portstoragetariffsmaster.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key for ShipPortTariffDtl
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipPortTariffDtlId implements Serializable {
    private Long transactionPoid;
    private Long detRowId;
}
