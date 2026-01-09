package com.asg.shipping.shippingmanifestcorrector.repository;

import com.asg.shipping.shippingmanifestcorrector.entity.ShipBlReprintChargeDtl;
import com.asg.shipping.shippingmanifestcorrector.entity.ShipBlReprintChargeDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for SHIP_BL_REPRINT_CHARGE_DTL
 */
@Repository
public interface ShipBlReprintChargeDtlRepository extends JpaRepository<ShipBlReprintChargeDtl, ShipBlReprintChargeDtlId> {

    @Query("SELECT d FROM ShipBlReprintChargeDtl d WHERE d.transactionPoid = :transactionPoid " +
           "ORDER BY d.detRowId")
    List<ShipBlReprintChargeDtl> findByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    @Modifying
    @Query("DELETE FROM ShipBlReprintChargeDtl d WHERE d.transactionPoid = :transactionPoid")
    void deleteByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    @Query("SELECT COALESCE(MAX(d.detRowId), 0) FROM ShipBlReprintChargeDtl d WHERE d.transactionPoid = :transactionPoid")
    Long findMaxDetRowIdByTransactionPoid(@Param("transactionPoid") Long transactionPoid);
}

