package com.asg.shipping.linepayabletransfetasperreporting.repository;

import com.asg.shipping.linepayabletransfetasperreporting.entity.ShipLineReportTransferHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for SHIP_LINE_REPORT_TRANSFER_HDR
 */
@Repository
public interface ShipLineReportTransferHdrRepository extends JpaRepository<ShipLineReportTransferHdr, Long> {

    @Query("SELECT h FROM ShipLineReportTransferHdr h WHERE h.transactionPoid = :transactionPoid " +
            "AND (h.deleted IS NULL OR h.deleted = 'N')")
    Optional<ShipLineReportTransferHdr> findActiveByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    @Query("SELECT h FROM ShipLineReportTransferHdr h WHERE h.docRef = :docRef " +
            "AND (h.deleted IS NULL OR h.deleted = 'N')")
    Optional<ShipLineReportTransferHdr> findByDocRef(@Param("docRef") String docRef);

    @Query("SELECT COUNT(h) > 0 FROM ShipLineReportTransferHdr h WHERE h.docRef = :docRef " +
            "AND (h.deleted IS NULL OR h.deleted = 'N') " +
            "AND (:excludeTransactionPoid IS NULL OR h.transactionPoid <> :excludeTransactionPoid)")
    boolean existsByDocRef(@Param("docRef") String docRef,
                           @Param("excludeTransactionPoid") Long excludeTransactionPoid);
}
