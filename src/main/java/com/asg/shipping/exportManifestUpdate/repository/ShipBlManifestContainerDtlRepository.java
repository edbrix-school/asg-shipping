package com.asg.shipping.exportManifestUpdate.repository;

import com.asg.shipping.exportManifestUpdate.entity.ShipBlManifestContainerDtl;
import com.asg.shipping.exportManifestUpdate.entity.ShipBlManifestContainerDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SHIP_BL_MANIFEST_CONTAINER_DTL
 */
@Repository
public interface ShipBlManifestContainerDtlRepository extends JpaRepository<ShipBlManifestContainerDtl, ShipBlManifestContainerDtlId> {

    List<ShipBlManifestContainerDtl> findByTransactionPoidOrderByDetRowId(Long transactionPoid);

    @Query("SELECT COALESCE(MAX(d.detRowId), 0) + 1 FROM ShipBlManifestContainerDtl d WHERE d.transactionPoid = :transactionPoid")
    Long getNextDetRowId(@Param("transactionPoid") Long transactionPoid);

    @Modifying
    @Query("DELETE FROM ShipBlManifestContainerDtl d WHERE d.transactionPoid = :transactionPoid AND d.detRowId IN :detRowIds")
    void deleteByTransactionPoidAndDetRowIds(@Param("transactionPoid") Long transactionPoid, @Param("detRowIds") List<Long> detRowIds);

    Optional<ShipBlManifestContainerDtl> findByTransactionPoidAndContainerNo(Long transactionPoid, String containerNo);

    @Query("SELECT COALESCE(SUM(d.grsVolume), 0), COALESCE(SUM(d.netVolume), 0), " +
           "COALESCE(SUM(d.grsWeight), 0), COALESCE(SUM(d.netWeight), 0), " +
           "COALESCE(SUM(d.noOfPacks), 0) FROM ShipBlManifestContainerDtl d " +
           "WHERE d.transactionPoid = :transactionPoid")
    Object[] calculateTotals(@Param("transactionPoid") Long transactionPoid);
}

