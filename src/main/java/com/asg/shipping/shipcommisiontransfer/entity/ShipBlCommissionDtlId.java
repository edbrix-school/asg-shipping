package com.asg.shipping.shipcommisiontransfer.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for ShipBlCommissionDtl
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipBlCommissionDtlId implements Serializable {

    private Long transactionPoid;
    private Long detRowId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShipBlCommissionDtlId that = (ShipBlCommissionDtlId) o;
        return Objects.equals(transactionPoid, that.transactionPoid) &&
                Objects.equals(detRowId, that.detRowId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionPoid, detRowId);
    }
}
