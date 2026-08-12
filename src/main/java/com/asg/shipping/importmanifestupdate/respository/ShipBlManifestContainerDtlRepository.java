package com.asg.shipping.importmanifestupdate.respository;

import com.asg.shipping.importmanifestupdate.entity.ShipBlManifestContainerDtl;
import com.asg.shipping.importmanifestupdate.entity.ShipBlManifestDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShipBlManifestContainerDtlRepository extends JpaRepository<ShipBlManifestContainerDtl, ShipBlManifestDtlId> {

    List<ShipBlManifestContainerDtl>
    findByIdTransactionPoidOrderByIdDetRowId(Long transactionPoid);

    Optional<ShipBlManifestContainerDtl>
    findByIdTransactionPoidAndContainerNo(Long transactionPoid, String containerNo);

    void deleteByIdTransactionPoid(Long transactionPoid);

    @Query("""
        SELECT COALESCE(MAX(d.id.detRowId), 0)
        FROM ShipBlManifestContainerDtl d
        WHERE d.id.transactionPoid = :transactionPoid
    """)
    Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);

    List<ShipBlManifestContainerDtl>
    findByIdTransactionPoid(Long transactionPoid);

    boolean existsByIdTransactionPoidAndContainerNo(Long transactionPoid, String containerNo);

    @Query("""
            select d.totalAmountCollected
            from ShipBlManifestContainerDtl d
            where d.id.transactionPoid = :transactionPoid
              and d.containerNo = :containerNo
            """)
    Optional<BigDecimal> findTotalAmountCollected(@Param("transactionPoid") Long transactionPoid, @Param("containerNo") String containerNo);

    /**
     * Force fresh data retrieval from database using native query
     * This bypasses any JPA caching to ensure we get the latest EXTRA_FREE_DAYS_PRNPLS values
     */
    @Query(value = """
        SELECT * FROM SHIP_BL_MANIFEST_CONTAINER_DTL
        WHERE TRANSACTION_POID = :transactionPoid
        ORDER BY DET_ROW_ID
        """, nativeQuery = true)
    List<ShipBlManifestContainerDtl> findByTransactionPoidWithFreshData(@Param("transactionPoid") Long transactionPoid);

    /**
     * Replaces the SH_CONTAINER_PART LOV lookup by POID with a direct query against the source table.
     */
    @Query(value = """
        SELECT TRANSACTION_POID AS POID, CONTAINER_NO AS CODE, CONTAINER_NO AS DESCRIPTION
        FROM SHIP_BL_MANIFEST_CONTAINER_DTL
        WHERE TRANSACTION_POID IN (:poids)
        """, nativeQuery = true)
    List<Object[]> findContainerPartLovByPoids(@Param("poids") List<Long> poids);

    /**
     * Replaces the SH_CONTAINER_PART LOV lookup by CODE with a direct query against the source table.
     */
    @Query(value = """
        SELECT TRANSACTION_POID AS POID, CONTAINER_NO AS CODE, CONTAINER_NO AS DESCRIPTION
        FROM SHIP_BL_MANIFEST_CONTAINER_DTL
        WHERE UPPER(CONTAINER_NO) = UPPER(:code)
        """, nativeQuery = true)
    List<Object[]> findContainerPartLovByCode(@Param("code") String code);

    /**
     * Batch form of {@link #findContainerPartLovByCode}, so a document with many part BLs resolves
     * every container number in one query instead of one per row.
     */
    @Query(value = """
        SELECT TRANSACTION_POID AS POID, CONTAINER_NO AS CODE, CONTAINER_NO AS DESCRIPTION
        FROM SHIP_BL_MANIFEST_CONTAINER_DTL
        WHERE UPPER(CONTAINER_NO) IN (:codes)
        """, nativeQuery = true)
    List<Object[]> findContainerPartLovByCodes(@Param("codes") List<String> codes);
}
