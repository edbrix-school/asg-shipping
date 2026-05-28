package com.asg.shipping.exportManifestUpdate.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite key for SHIP_BL_MANIFEST_CARGO_DTL
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportShipBlManifestCargoDtlId implements Serializable {

    private Long transactionPoid;

    private Long detRowId;

    private String descriptionType;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExportShipBlManifestCargoDtlId that = (ExportShipBlManifestCargoDtlId) o;
        return Objects.equals(transactionPoid, that.transactionPoid) &&
                Objects.equals(detRowId, that.detRowId) &&
                Objects.equals(descriptionType, that.descriptionType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionPoid, detRowId, descriptionType);
    }
}

