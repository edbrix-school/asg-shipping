package com.asg.shipping.linetariffs.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for ShipLineTariffImpDtl
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipLineTariffImpDtlId implements Serializable {

    private Long transactionPoid;
    private Long detRowId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShipLineTariffImpDtlId that = (ShipLineTariffImpDtlId) o;
        return Objects.equals(transactionPoid, that.transactionPoid) &&
               Objects.equals(detRowId, that.detRowId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionPoid, detRowId);
    }
}

