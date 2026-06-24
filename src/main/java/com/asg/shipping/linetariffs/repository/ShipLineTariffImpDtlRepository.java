package com.asg.shipping.linetariffs.repository;

import com.asg.shipping.linetariffs.entity.ShipLineTariffImpDtl;
import com.asg.shipping.linetariffs.entity.ShipLineTariffImpDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ShipLineTariffImpDtl entity (Import Demurrage Collectable)
 */
@Repository
public interface ShipLineTariffImpDtlRepository extends JpaRepository<ShipLineTariffImpDtl, ShipLineTariffImpDtlId> {

    /**
     * Find all detail records for a transaction, ordered by DET_ROW_ID
     */
    @Query("SELECT d FROM ShipLineTariffImpDtl d WHERE d.transactionPoid = :transactionPoid ORDER BY d.detRowId")
    List<ShipLineTariffImpDtl> findByTransactionPoidOrderByDetRowId(@Param("transactionPoid") Long transactionPoid);

    /**
     * Find detail record by transaction and det row id
     */
    Optional<ShipLineTariffImpDtl> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);

    /**
     * Delete all detail records for a transaction
     */
    void deleteByTransactionPoid(Long transactionPoid);

    /**
     * Get max DET_ROW_ID for a transaction (for generating new DET_ROW_ID)
     */
    @Query("SELECT COALESCE(MAX(d.detRowId), 0) FROM ShipLineTariffImpDtl d WHERE d.transactionPoid = :transactionPoid")
    Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO SHIP_LINE_TARIFF_IMP_DTL (TRANSACTION_POID, DET_ROW_ID, CONTAINER_TYPE_POID)
            SELECT :transactionPoid,
                   (SELECT COALESCE(MAX(det_row_id), 0) FROM SHIP_LINE_TARIFF_IMP_DTL WHERE TRANSACTION_POID = :transactionPoid) + ROWNUM,
                   CONTAINER_TYPE_POID
            FROM SHIP_LINE_MASTER_TYPE_DTL
            WHERE LINE_POID = :linePoid
              AND CONTAINER_TYPE_POID IS NOT NULL
              AND (VALID_UNTIL IS NULL OR VALID_UNTIL >= SYSDATE)
              AND CONTAINER_TYPE_POID NOT IN (
                  SELECT COALESCE(CONTAINER_TYPE_POID, 0) FROM SHIP_LINE_TARIFF_IMP_DTL WHERE TRANSACTION_POID = :transactionPoid
              )
            """, nativeQuery = true)
    void bulkInsertFromLine(@Param("transactionPoid") Long transactionPoid, @Param("linePoid") Long linePoid);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM SHIP_LINE_TARIFF_IMP_DTL WHERE TRANSACTION_POID = :transactionPoid AND DET_ROW_ID IN (:detRowIds)", nativeQuery = true)
    void deleteByTransactionPoidAndDetRowIds(@Param("transactionPoid") Long transactionPoid, @Param("detRowIds") List<Long> detRowIds);
}
