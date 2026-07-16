package com.asg.shipping.exportManifestUpdate.repository;

import com.asg.shipping.exportManifestUpdate.entity.ExportShipBlManifestHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SHIP_BL_MANIFEST_HDR
 */
@Repository
public interface ExportShipBlManifestHdrRepository extends JpaRepository<ExportShipBlManifestHdr, Long> {

    Optional<ExportShipBlManifestHdr> findByTransactionPoidAndGroupPoidAndCompanyPoidAndDeleted(
            Long transactionPoid, Long groupPoid, Long companyPoid, String deleted);

    /**
     * Find active Export BL for export-manifest-update (100-352):
     * - active (DELETED = 'N' or NULL)
     * - BL_TYPE = 'EXPORT'
     * - GROUP_POID and COMPANY_POID must match
     */
    @Query("SELECT h FROM ExportShipBlManifestHdr h WHERE h.transactionPoid = :transactionPoid " +
           "AND h.groupPoid = :groupPoid AND h.companyPoid = :companyPoid " +
           "AND h.blType = 'EXPORT'")
    Optional<ExportShipBlManifestHdr> findExportBlByTransactionPoid(
            @Param("transactionPoid") Long transactionPoid,
            @Param("groupPoid") Long groupPoid,
            @Param("companyPoid") Long companyPoid);

    /**
     * Find active Export BL by transaction POID — aligned with export-manifest-bl (100-104).
     */
    @Query("SELECT h FROM ExportShipBlManifestHdr h WHERE h.transactionPoid = :transactionPoid " +
           "AND h.blType = 'EXPORT' AND (h.deleted IS NULL OR h.deleted = 'N')")
    Optional<ExportShipBlManifestHdr> findActiveExportBlByTransactionPoid(
            @Param("transactionPoid") Long transactionPoid);

    List<ExportShipBlManifestHdr> findByGroupPoidAndCompanyPoidAndDeletedAndBlType(
            Long groupPoid, Long companyPoid, String deleted, String blType);

    Optional<ExportShipBlManifestHdr> findByVoyageTransactionPoidAndBlNumberAndDeleted(
            Long voyageTransactionPoid, String blNumber, String deleted);

    @Query("SELECT h FROM ExportShipBlManifestHdr h WHERE h.groupPoid = :groupPoid " +
           "AND h.companyPoid = :companyPoid AND h.deleted = :deleted " +
           "AND h.blType = 'EXPORT' " +
           "AND (:blNumber IS NULL OR h.blNumber LIKE %:blNumber%) " +
           "AND (:blStatus IS NULL OR h.blStatus = :blStatus) " +
           "AND (:voyageTransactionPoid IS NULL OR h.voyageTransactionPoid = :voyageTransactionPoid)")
    List<ExportShipBlManifestHdr> findExportBlsWithFilters(
            @Param("groupPoid") Long groupPoid,
            @Param("companyPoid") Long companyPoid,
            @Param("deleted") String deleted,
            @Param("blNumber") String blNumber,
            @Param("blStatus") String blStatus,
            @Param("voyageTransactionPoid") Long voyageTransactionPoid);

    boolean existsByBlNumberAndVoyageTransactionPoidAndDeleted(String blNumber, Long voyageTransactionPoid, String deleted);
}

