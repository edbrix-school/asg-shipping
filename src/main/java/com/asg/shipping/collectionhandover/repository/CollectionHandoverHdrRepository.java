package com.asg.shipping.collectionhandover.repository;

import com.asg.shipping.collectionhandover.entity.ArShDayEndCloseHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface CollectionHandoverHdrRepository extends JpaRepository<ArShDayEndCloseHdr, Long> {

    Optional<ArShDayEndCloseHdr> findByTransactionPoidAndGroupPoid(Long transactionPoid, Long groupPoid);

    @Query("SELECT COUNT(h) > 0 FROM CollectionHandoverHdr h " +
           "WHERE h.docRef = :docRef AND h.deleted = 'N'")
    boolean existsByDocRef(@Param("docRef") String docRef);

    @Query("SELECT COUNT(h) > 0 FROM CollectionHandoverHdr h " +
           "WHERE h.docRef = :docRef AND h.transactionPoid != :excludeTransactionPoid AND h.deleted = 'N'")
    boolean existsByDocRefExcludingPoid(@Param("docRef") String docRef, @Param("excludeTransactionPoid") Long excludeTransactionPoid);

    @Query(value = """
                SELECT COUNT(*)
                FROM AR_SH_DAY_END_CLOSE_HDR
                WHERE TRANSACTION_DATE = :transactionDate
                  AND GROUP_POID = :groupPoid
                  AND COMPANY_POID = :companyPoid
                  AND (DELETED IS NULL OR DELETED = 'N')
                  AND (TOTAL_AMOUNT IS NULL OR TOTAL_AMOUNT >= 0)
            """, nativeQuery = true)
    Long countByTransactionDateAndGroupPoidAndCompanyPoid(
            @Param("transactionDate") LocalDate transactionDate,
            @Param("groupPoid") Long groupPoid,
            @Param("companyPoid") Long companyPoid);
}