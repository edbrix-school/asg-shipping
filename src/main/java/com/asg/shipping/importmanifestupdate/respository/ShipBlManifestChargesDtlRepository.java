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
     */
    @Query(value = """
        SELECT DISTINCT recphdr.TRANSACTION_POID AS POID, recphdr.DOC_REF AS CODE, SUBSTR(dtl.DOC_REF_LINK_NO, 15, 7) AS DESCRIPTION
        FROM SHIP_BL_MANIFEST_CHARGES_DTL dtl
        INNER JOIN AR_SH_RECEIPT_HDR recphdr ON dtl.RECEIPT_INVOICE_POID = recphdr.TRANSACTION_POID
        WHERE dtl.RECEIPT_INVOICE_POID IN (:poids)
          AND SUBSTR(dtl.DOC_REF_LINK_NO, 15, 7) = '300-103'
        UNION ALL
        SELECT DISTINCT invhdr.TRANSACTION_POID AS POID, invhdr.DOC_REF AS CODE, SUBSTR(dtl.DOC_REF_LINK_NO, 15, 7) AS DESCRIPTION
        FROM SHIP_BL_MANIFEST_CHARGES_DTL dtl
        INNER JOIN AR_SH_SALES_INVOICE_HDR invhdr ON dtl.RECEIPT_INVOICE_POID = invhdr.TRANSACTION_POID
        WHERE dtl.RECEIPT_INVOICE_POID IN (:poids)
          AND SUBSTR(dtl.DOC_REF_LINK_NO, 15, 7) = '300-102'
        """, nativeQuery = true)
    List<Object[]> findReceiptInvoiceLovByPoids(@Param("poids") List<Long> poids);
}
