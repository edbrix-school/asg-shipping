package com.asg.shipping.shippingmanifestcorrector.repository;

import com.asg.shipping.shippingmanifestcorrector.entity.ShipBlReprintContainerDtl;
import com.asg.shipping.shippingmanifestcorrector.entity.ShipBlReprintContainerDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for SHIP_BL_REPRINT_CONTAINER_DTL
 */
@Repository
public interface ShipBlReprintContainerDtlRepository extends JpaRepository<ShipBlReprintContainerDtl, ShipBlReprintContainerDtlId> {

    @Query("SELECT d FROM ShipBlReprintContainerDtl d WHERE d.transactionPoid = :transactionPoid " +
           "ORDER BY d.detRowId")
    List<ShipBlReprintContainerDtl> findByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    @Modifying
    @Query("DELETE FROM ShipBlReprintContainerDtl d WHERE d.transactionPoid = :transactionPoid")
    void deleteByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    @Query("SELECT COALESCE(MAX(d.detRowId), 0) FROM ShipBlReprintContainerDtl d WHERE d.transactionPoid = :transactionPoid")
    Long findMaxDetRowIdByTransactionPoid(@Param("transactionPoid") Long transactionPoid);
}

