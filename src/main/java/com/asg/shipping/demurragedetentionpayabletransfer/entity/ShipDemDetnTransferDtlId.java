package com.asg.shipping.demurragedetentionpayabletransfer.entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for ShipDemDetnTransferDtl
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipDemDetnTransferDtlId implements Serializable {

    private Long transactionPoid;
    private Long detRowId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShipDemDetnTransferDtlId that = (ShipDemDetnTransferDtlId) o;
        return Objects.equals(transactionPoid, that.transactionPoid) &&
                Objects.equals(detRowId, that.detRowId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionPoid, detRowId);
    }
}
