package com.asg.shipping.exportManifestUpdate.repository;

import com.asg.shipping.exportManifestUpdate.entity.ShipBlManifestChargesDtl;
import com.asg.shipping.exportManifestUpdate.entity.ShipBlManifestChargesDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for SHIP_BL_MANIFEST_CHARGES_DTL
 */
@Repository
public interface ShipBlManifestChargesDtlRepository extends JpaRepository<ShipBlManifestChargesDtl, ShipBlManifestChargesDtlId> {

    List<ShipBlManifestChargesDtl> findByTransactionPoidOrderByDetRowId(Long transactionPoid);

    @Query("SELECT COALESCE(MAX(d.detRowId), 0) + 1 FROM ShipBlManifestChargesDtl d WHERE d.transactionPoid = :transactionPoid")
    Long getNextDetRowId(@Param("transactionPoid") Long transactionPoid);

    @Modifying
    @Query("DELETE FROM ShipBlManifestChargesDtl d WHERE d.transactionPoid = :transactionPoid AND d.detRowId IN :detRowIds")
    void deleteByTransactionPoidAndDetRowIds(@Param("transactionPoid") Long transactionPoid, @Param("detRowIds") List<Long> detRowIds);

    @Query("SELECT COALESCE(SUM(d.buyPercharge * d.quantity), 0) as buyAmount, " +
           "COALESCE(SUM(d.perQuantityAmount * d.quantity), 0) as saleAmount, " +
           "COALESCE(SUM((d.perQuantityAmount - d.buyPercharge) * d.quantity), 0) as gainAmount, " +
           "COALESCE(SUM(d.taxAmount), 0) as vatAmount " +
           "FROM ShipBlManifestChargesDtl d WHERE d.transactionPoid = :transactionPoid")
    Object[] calculateTotals(@Param("transactionPoid") Long transactionPoid);
}

