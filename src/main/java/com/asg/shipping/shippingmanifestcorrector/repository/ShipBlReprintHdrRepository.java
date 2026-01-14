package com.asg.shipping.shippingmanifestcorrector.repository;

import com.asg.shipping.shippingmanifestcorrector.entity.ShipBlReprintHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SHIP_BL_REPRINT_HDR
 */
@Repository
public interface ShipBlReprintHdrRepository extends JpaRepository<ShipBlReprintHdr, Long> {

    @Query("SELECT h FROM ShipBlReprintHdr h WHERE h.transactionPoid = :transactionPoid " +
           "AND (h.deleted IS NULL OR h.deleted = 'N')")
    Optional<ShipBlReprintHdr> findActiveByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    @Query("SELECT h FROM ShipBlReprintHdr h WHERE h.blNumber = :blNumber " +
           "AND (h.deleted IS NULL OR h.deleted = 'N')")
    List<ShipBlReprintHdr> findByBlNumber(@Param("blNumber") String blNumber);

    @Query("SELECT h FROM ShipBlReprintHdr h WHERE h.docRef = :docRef " +
           "AND (h.deleted IS NULL OR h.deleted = 'N')")
    Optional<ShipBlReprintHdr> findByDocRef(@Param("docRef") String docRef);
}

