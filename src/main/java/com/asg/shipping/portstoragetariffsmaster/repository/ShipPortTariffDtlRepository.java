package com.asg.shipping.portstoragetariffsmaster.repository;


import com.asg.shipping.portstoragetariffsmaster.entity.ShipPortTariffDtl;
import com.asg.shipping.portstoragetariffsmaster.entity.ShipPortTariffDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ShipPortTariffDtl entity
 */
@Repository
public interface ShipPortTariffDtlRepository extends JpaRepository<ShipPortTariffDtl, ShipPortTariffDtlId> {

    /**
     * Find all detail records for a transaction POID
     */
    List<ShipPortTariffDtl> findByTransactionPoidOrderByDetRowId(Long transactionPoid);

    /**
     * Delete all detail records for a transaction POID
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ShipPortTariffDtl d WHERE d.transactionPoid = :transactionPoid")
    void deleteByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    /**
     * Find detail by transaction POID and det row ID
     */
    Optional<ShipPortTariffDtl> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);
}
