package com.asg.shipping.exportManifestBl.repository;

import com.asg.shipping.exportManifestBl.entity.ExportManifestBlCargoDtl;
import com.asg.shipping.importmanifestupdate.entity.ShipBlManifestCargoDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ExportManifestBlCargoDtlRepository extends JpaRepository<ExportManifestBlCargoDtl, ShipBlManifestCargoDtlId> {

    List<ExportManifestBlCargoDtl> findById_TransactionPoid(Long transactionPoid);

    List<ExportManifestBlCargoDtl> findById_TransactionPoidAndId_DescriptionTypeOrderById_DetRowId(
            Long transactionPoid, String descriptionType);

    void deleteById_TransactionPoid(Long transactionPoid);

    @Modifying
    @Query("DELETE FROM ExportManifestBlCargoDtl d WHERE d.id.transactionPoid = :transactionPoid "
            + "AND d.id.descriptionType = :descriptionType")
    void deleteAllByTransactionPoidAndDescriptionType(
            @Param("transactionPoid") Long transactionPoid,
            @Param("descriptionType") String descriptionType);

    @Query(value = "SELECT CARGO_DESCRIPTION FROM SHIP_BL_MANIFEST_CARGO_DTL "
            + "WHERE TRANSACTION_POID = :transactionPoid "
            + "AND DESCRIPTION_TYPE = :descriptionType "
            + "ORDER BY DET_ROW_ID", nativeQuery = true)
    List<String> findDescriptionStringsByType(
            @Param("transactionPoid") Long transactionPoid,
            @Param("descriptionType") String descriptionType);
}
