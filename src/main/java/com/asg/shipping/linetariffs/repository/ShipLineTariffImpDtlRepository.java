package com.asg.shipping.linetariffs.repository;

import com.asg.shipping.linetariffs.entity.ShipLineTariffImpDtl;
import com.asg.shipping.linetariffs.entity.ShipLineTariffImpDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
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
    List<ShipLineTariffImpDtl> findByTransactionPoidOrderByDetRowId(Long transactionPoid);

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
}

