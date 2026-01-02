package com.asg.shipping.demurragedetentionpayabletransfer.repository;

import com.asg.shipping.demurragedetentionpayabletransfer.entity.ShipDemDetnTransferHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for ShipDemDetnTransferHdr entity
 */
@Repository
public interface ShipDemDetnTransferHdrRepository extends JpaRepository<ShipDemDetnTransferHdr, Long> {

    Optional<ShipDemDetnTransferHdr> findByTransactionPoidAndGroupPoidAndCompanyPoid(
            Long transactionPoid, Long groupPoid, Long companyPoid);

    Optional<ShipDemDetnTransferHdr> findByDocRefAndDeletedNot(String docRef, String deleted);

    @Query("SELECT COUNT(h) > 0 FROM ShipDemDetnTransferHdr h WHERE h.docRef = :docRef AND h.deleted != 'Y'")
    boolean existsByDocRefAndNotDeleted(@Param("docRef") String docRef);
}
