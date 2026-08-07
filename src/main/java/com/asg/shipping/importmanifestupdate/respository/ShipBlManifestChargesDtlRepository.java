package com.asg.shipping.importmanifestupdate.respository;

import com.asg.shipping.importmanifestupdate.entity.ShipBlManifestChargesDtl;
import com.asg.shipping.importmanifestupdate.entity.ShipBlManifestDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipBlManifestChargesDtlRepository  extends JpaRepository<ShipBlManifestChargesDtl, ShipBlManifestDtlId> {

    List<ShipBlManifestChargesDtl>
    findByIdTransactionPoidOrderByIdDetRowId(Long transactionPoid);

    void deleteByIdTransactionPoid(Long transactionPoid);

    @Query("""
        SELECT COALESCE(MAX(d.id.detRowId), 0)
        FROM ShipBlManifestChargesDtl d
        WHERE d.id.transactionPoid = :transactionPoid
    """)
    Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);

    /**
     * Replaces the MANIFEST_RECEIPT_INVOICE LOV lookup by POID with a direct query against the source tables.
     * The original LOV's DOC_REF_LINK_NO/SUBSTR gating only makes sense for the browse/search path
     * (finding which charges reference a given receipt/invoice); resolving an already-known
     * receiptInvoicePoid just needs a direct header lookup, independent of any charges-detail row.
     */
    @Query(value = """
        SELECT TRANSACTION_POID AS POID, DOC_REF AS CODE, '300-103' AS DESCRIPTION
        FROM AR_SH_RECEIPT_HDR
        WHERE TRANSACTION_POID IN (:poids)
        UNION ALL
        SELECT TRANSACTION_POID AS POID, DOC_REF AS CODE, '300-102' AS DESCRIPTION
        FROM AR_SH_SALES_INVOICE_HDR
        WHERE TRANSACTION_POID IN (:poids)
        """, nativeQuery = true)
    List<Object[]> findReceiptInvoiceLovByPoids(@Param("poids") List<Long> poids);
}
