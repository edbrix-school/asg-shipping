package com.asg.shipping.exportManifestUpdate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite key for SHIP_BL_MANIFEST_CARGO_DTL
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportShipBlManifestCargoDtlId implements Serializable {

    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "DESCRIPTION_TYPE", nullable = false, length = 10)
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

