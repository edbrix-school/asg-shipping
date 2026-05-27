package com.asg.shipping.exportManifestUpdate.repository;

import com.asg.shipping.exportManifestUpdate.entity.ExportShipBlManifestCargoDtl;
import com.asg.shipping.exportManifestUpdate.entity.ExportShipBlManifestCargoDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for SHIP_BL_MANIFEST_CARGO_DTL
 */
@Repository
public interface ExportShipBlManifestCargoDtlRepository extends JpaRepository<ExportShipBlManifestCargoDtl, ExportShipBlManifestCargoDtlId> {

    List<ExportShipBlManifestCargoDtl> findByTransactionPoidAndDescriptionTypeOrderByDetRowId(
            Long transactionPoid, String descriptionType);

    @Query(value = "SELECT * FROM SHIP_BL_MANIFEST_CARGO_DTL " +
                   "WHERE TRANSACTION_POID = :transactionPoid " +
                   "AND DESCRIPTION_TYPE = :descriptionType " +
                   "ORDER BY DET_ROW_ID", nativeQuery = true)
    List<ExportShipBlManifestCargoDtl> findCargoRowsByType(
            @Param("transactionPoid") Long transactionPoid,
            @Param("descriptionType") String descriptionType);

    @Query("SELECT COALESCE(MAX(d.detRowId), 0) + 1 FROM ExportShipBlManifestCargoDtl d WHERE d.transactionPoid = :transactionPoid AND d.descriptionType = :descriptionType")
    Long getNextDetRowId(@Param("transactionPoid") Long transactionPoid, @Param("descriptionType") String descriptionType);

    @Modifying
    @Query("DELETE FROM ExportShipBlManifestCargoDtl d WHERE d.transactionPoid = :transactionPoid " +
           "AND d.descriptionType = :descriptionType AND d.detRowId IN :detRowIds")
    void deleteByTransactionPoidAndDescriptionTypeAndDetRowIds(
            @Param("transactionPoid") Long transactionPoid,
            @Param("descriptionType") String descriptionType,
            @Param("detRowIds") List<Long> detRowIds);

    @Modifying
    @Query("DELETE FROM ExportShipBlManifestCargoDtl d WHERE d.transactionPoid = :transactionPoid " +
           "AND d.descriptionType = :descriptionType")
    void deleteAllByTransactionPoidAndDescriptionType(
            @Param("transactionPoid") Long transactionPoid,
            @Param("descriptionType") String descriptionType);
}

