package com.asg.shipping.exportManifestUpdate.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite key for SHIP_BL_MANIFEST_CHARGES_DTL
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportShipBlManifestChargesDtlId implements Serializable {

    private Long transactionPoid;

    private Long detRowId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExportShipBlManifestChargesDtlId that = (ExportShipBlManifestChargesDtlId) o;
        return Objects.equals(transactionPoid, that.transactionPoid) &&
                Objects.equals(detRowId, that.detRowId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionPoid, detRowId);
    }
}

