package com.asg.shipping.exportManifestBl.repository;

import com.asg.shipping.exportManifestBl.entity.ExportManifestBlHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExportManifestBlHdrRepository extends JpaRepository<ExportManifestBlHdr, Long> {

    
    @Query("SELECT h FROM ExportManifestBlHdr h WHERE h.transactionPoid = :transactionPoid " +
            "AND h.blType = 'EXPORT' AND (h.deleted IS NULL OR h.deleted = 'N')")
    Optional<ExportManifestBlHdr> findByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

   
    @Query("SELECT COUNT(h) > 0 FROM ExportManifestBlHdr h " +
            "WHERE h.voyageTransactionPoid = :voyageTransactionPoid " +
            "AND h.blNumber = :blNumber " +
            "AND h.transactionPoid != :excludeTransactionPoid " +
            "AND h.blType = 'EXPORT' " +
            "AND (h.deleted IS NULL OR h.deleted = 'N')")
    boolean existsByVoyageTransactionPoidAndBlNumberExcludingPoid(
            @Param("voyageTransactionPoid") Long voyageTransactionPoid,
            @Param("blNumber") String blNumber,
            @Param("excludeTransactionPoid") Long excludeTransactionPoid);

    
    @Query("SELECT COUNT(h) > 0 FROM ExportManifestBlHdr h " +
            "WHERE h.blNumber = :blNumber " +
            "AND h.transactionPoid != :excludeTransactionPoid " +
            "AND h.blType = 'EXPORT' " +
            "AND (h.deleted IS NULL OR h.deleted = 'N')")
    boolean existsByBlNumberExcludingPoid(
            @Param("blNumber") String blNumber,
            @Param("excludeTransactionPoid") Long excludeTransactionPoid);

   
    @Query("SELECT COUNT(h) > 0 FROM ExportManifestBlHdr h " +
            "WHERE h.voyageTransactionPoid = :voyageTransactionPoid " +
            "AND h.blNumber = :blNumber " +
            "AND h.blType = 'EXPORT' " +
            "AND (h.deleted IS NULL OR h.deleted = 'N')")
    boolean existsByVoyageTransactionPoidAndBlNumber(
            @Param("voyageTransactionPoid") Long voyageTransactionPoid,
            @Param("blNumber") String blNumber);

  
    @Query("SELECT COUNT(h) > 0 FROM ExportManifestBlHdr h " +
            "WHERE h.blNumber = :blNumber " +
            "AND h.blType = 'EXPORT' " +
            "AND (h.deleted IS NULL OR h.deleted = 'N')")
    boolean existsByBlNumber(@Param("blNumber") String blNumber);
}

