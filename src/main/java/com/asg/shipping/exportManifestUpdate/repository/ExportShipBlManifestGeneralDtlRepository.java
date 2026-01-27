package com.asg.shipping.exportManifestUpdate.repository;

import com.asg.shipping.exportManifestUpdate.entity.ExportShipBlManifestGeneralDtl;
import com.asg.shipping.exportManifestUpdate.entity.ExportShipBlManifestGeneralDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for SHIP_BL_MANIFEST_GENERAL_DTL
 */
@Repository
public interface ExportShipBlManifestGeneralDtlRepository extends JpaRepository<ExportShipBlManifestGeneralDtl, ExportShipBlManifestGeneralDtlId> {

    List<ExportShipBlManifestGeneralDtl> findByTransactionPoidOrderByDetRowId(Long transactionPoid);

    @Query("SELECT COALESCE(MAX(d.detRowId), 0) + 1 FROM ExportShipBlManifestGeneralDtl d WHERE d.transactionPoid = :transactionPoid")
    Long getNextDetRowId(@Param("transactionPoid") Long transactionPoid);

    @Modifying
    @Query("DELETE FROM ExportShipBlManifestGeneralDtl d WHERE d.transactionPoid = :transactionPoid AND d.detRowId IN :detRowIds")
    void deleteByTransactionPoidAndDetRowIds(@Param("transactionPoid") Long transactionPoid, @Param("detRowIds") List<Long> detRowIds);

    @Query("SELECT COALESCE(SUM(d.grsVolume), 0), COALESCE(SUM(d.netVolume), 0), " +
           "COALESCE(SUM(d.grsWeight), 0), COALESCE(SUM(d.netWeight), 0), " +
           "COALESCE(SUM(d.noOfPacks), 0) FROM ExportShipBlManifestGeneralDtl d " +
           "WHERE d.transactionPoid = :transactionPoid")
    Object[] calculateTotals(@Param("transactionPoid") Long transactionPoid);
}

