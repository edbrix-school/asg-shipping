package com.asg.shipping.linetariffs.repository;

import com.asg.shipping.linetariffs.entity.ShipLineTariffExpDtl;
import com.asg.shipping.linetariffs.entity.ShipLineTariffExpDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ShipLineTariffExpDtl entity (Export Detention Collectable)
 */
@Repository
public interface ShipLineTariffExpDtlRepository extends JpaRepository<ShipLineTariffExpDtl, ShipLineTariffExpDtlId> {

    /**
     * Find all detail records for a transaction, ordered by DET_ROW_ID
     */
    List<ShipLineTariffExpDtl> findByTransactionPoidOrderByDetRowId(Long transactionPoid);

    /**
     * Find detail record by transaction and det row id
     */
    Optional<ShipLineTariffExpDtl> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);

    /**
     * Delete all detail records for a transaction
     */
    void deleteByTransactionPoid(Long transactionPoid);

    /**
     * Get max DET_ROW_ID for a transaction (for generating new DET_ROW_ID)
     */
    @Query("SELECT COALESCE(MAX(d.detRowId), 0) FROM ShipLineTariffExpDtl d WHERE d.transactionPoid = :transactionPoid")
    Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);
}

