package com.asg.shipping.importManifestUpdate.respository;

import com.asg.shipping.importManifestUpdate.entity.ShipBlManifestHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipBlManifestHdrRepository extends JpaRepository<ShipBlManifestHdr, Long> {

    /**
     * Find BL manifest by TRANSACTION_POID, GROUP_POID, and COMPANY_POID
     */
    Optional<ShipBlManifestHdr> findByTransactionPoid(Long transactionPoid);

    /**
     * Check if BL number already exists for a voyage
     */

  /*  @Query("SELECT COUNT(h) > 0 FROM ShipBlManifestHdr h " +
            "WHERE h.voyageTransactionPoid = :voyageTransactionPoid " +
            "AND h.blNumber = :blNumber " +
            "AND h.deleted = 'N'")
    boolean existsByVoyageTransactionPoidAndBlNumber(
            @Param("voyageTransactionPoid") Long voyageTransactionPoid,
            @Param("blNumber") String blNumber);*/

    /**
     * Check if BL number already exists for a voyage excluding a specific TRANSACTION_POID (for updates)
     */
    @Query("SELECT COUNT(h) > 0 FROM ShipBlManifestHdr h " +
            "WHERE h.voyageTransactionPoid = :voyageTransactionPoid " +
            "AND h.blNumber = :blNumber " +
            "AND h.transactionPoid != :excludeTransactionPoid " +
            "AND h.deleted = 'N'")
    boolean existsByVoyageTransactionPoidAndBlNumberExcludingPoid(
            @Param("voyageTransactionPoid") Long voyageTransactionPoid,
            @Param("blNumber") String blNumber,
            @Param("excludeTransactionPoid") Long excludeTransactionPoid);

    /**
     * Check if BL number already exists globally (for PROC_BL_EXPORT_DUPLICATE equivalent)
     */

 /*   @Query("SELECT COUNT(h) > 0 FROM ShipBlManifestHdr h " +
            "WHERE h.blNumber = :blNumber " +
            "AND h.deleted = 'N'")
    boolean existsByBlNumber(@Param("blNumber") String blNumber);*/

    /**
     * Check if BL number already exists globally excluding a specific TRANSACTION_POID (for updates)
     */
    @Query("SELECT COUNT(h) > 0 FROM ShipBlManifestHdr h " +
            "WHERE h.blNumber = :blNumber " +
            "AND h.transactionPoid != :excludeTransactionPoid " +
            "AND h.deleted = 'N'")
    boolean existsByBlNumberExcludingPoid(
            @Param("blNumber") String blNumber,
            @Param("excludeTransactionPoid") Long excludeTransactionPoid);

    boolean existsByBlNumber(String trim);

    boolean existsByVoyageTransactionPoidAndBlNumber(Long voyageTransactionPoid, String trim);

    /**
     * Check if DOC_REF already exists
     */

   /* @Query("SELECT COUNT(h) > 0 FROM ShipBlManifestHdr h " +
            "WHERE h.docRef = :docRef AND h.deleted = 'N'")
    boolean existsByDocRef(@Param("docRef") String docRef);*/

    /**
     * Check if DOC_REF already exists excluding a specific TRANSACTION_POID (for updates)
     */

   /* @Query("SELECT COUNT(h) > 0 FROM ShipBlManifestHdr h " +
            "WHERE h.docRef = :docRef AND h.transactionPoid != :excludeTransactionPoid AND h.deleted = 'N'")
    boolean existsByDocRefExcludingPoid(@Param("docRef") String docRef, @Param("excludeTransactionPoid") Long excludeTransactionPoid);*/
}
