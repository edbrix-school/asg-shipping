package com.asg.shipping.collectionhandover.repository;

import com.asg.shipping.collectionhandover.entity.ArShDayEndCloseHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for ArShDayEndCloseHdr entity in Collection Handover context
 */
@Repository
public interface CollectionHandoverHdrRepository extends JpaRepository<ArShDayEndCloseHdr, Long> {

    /**
     * Find handover by TRANSACTION_POID and GROUP_POID
     */
    Optional<ArShDayEndCloseHdr> findByTransactionPoidAndGroupPoid(Long transactionPoid, Long groupPoid);

    /**
     * Check if DOC_REF already exists
     */
    @Query("SELECT COUNT(h) > 0 FROM ArShDayEndCloseHdr h " +
           "WHERE h.docRef = :docRef AND h.deleted = 'N'")
    boolean existsByDocRef(@Param("docRef") String docRef);

    /**
     * Check if DOC_REF already exists excluding a specific TRANSACTION_POID (for updates)
     */
    @Query("SELECT COUNT(h) > 0 FROM ArShDayEndCloseHdr h " +
           "WHERE h.docRef = :docRef AND h.transactionPoid != :excludeTransactionPoid AND h.deleted = 'N'")
    boolean existsByDocRefExcludingPoid(@Param("docRef") String docRef, @Param("excludeTransactionPoid") Long excludeTransactionPoid);
}