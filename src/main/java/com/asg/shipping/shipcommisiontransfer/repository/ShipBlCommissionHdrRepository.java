package com.asg.shipping.shipcommisiontransfer.repository;


import com.asg.shipping.shipcommisiontransfer.entity.ShipBlCommissionHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for ShipBlCommissionHdr entity
 */
@Repository
public interface ShipBlCommissionHdrRepository extends JpaRepository<ShipBlCommissionHdr, Long> {

    Optional<ShipBlCommissionHdr> findByTransactionPoidAndGroupPoidAndCompanyPoid(
            Long transactionPoid, Long groupPoid, Long companyPoid);

    Optional<ShipBlCommissionHdr> findByDocRefAndDeletedNot(String docRef, String deleted);

    @Query("SELECT COUNT(h) > 0 FROM ShipBlCommissionHdr h WHERE h.docRef = :docRef AND h.deleted != 'Y'")
    boolean existsByDocRefAndNotDeleted(@Param("docRef") String docRef);
}
