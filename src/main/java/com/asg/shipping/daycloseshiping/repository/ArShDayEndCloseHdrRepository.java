package com.asg.shipping.daycloseshiping.repository;

import com.asg.shipping.daycloseshiping.entity.ArShDayEndCloseHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ArShDayEndCloseHdrRepository extends JpaRepository<ArShDayEndCloseHdr, Long> {

    @Query(value = """
                SELECT COUNT(*)
                FROM AR_SH_DAY_END_CLOSE_HDR
                WHERE TRANSACTION_DATE = :transactionDate
                  AND GROUP_POID = :groupPoid
                  AND COMPANY_POID = :companyPoid
                  AND (DELETED IS NULL OR DELETED = 'N')
                  AND (TOTAL_AMOUNT IS NULL OR TOTAL_AMOUNT >= 0)
            """, nativeQuery = true)
    Long countByTransactionDateAndGroupPoidAndCompanyPoid(@Param("transactionDate") LocalDate transactionDate,
                                                          @Param("groupPoid") Long groupPoid, @Param("companyPoid") Long companyPoid);

    @Query(value = """
                SELECT COUNT(*)
                FROM AR_SH_DAY_END_CLOSE_HDR
                WHERE TRANSACTION_DATE = :transactionDate
                  AND GROUP_POID = :groupPoid
                  AND COMPANY_POID = :companyPoid
                  AND TRANSACTION_POID <> :excludeTransactionPoid
                  AND (DELETED IS NULL OR DELETED = 'N')
                  AND (TOTAL_AMOUNT IS NULL OR TOTAL_AMOUNT >= 0)
            """, nativeQuery = true)
    Long countByTransactionDateAndGroupPoidAndCompanyPoidExcludingTransactionPoid(
            @Param("transactionDate") LocalDate transactionDate,
            @Param("groupPoid") Long groupPoid,
            @Param("companyPoid") Long companyPoid,
            @Param("excludeTransactionPoid") Long excludeTransactionPoid);

    Optional<ArShDayEndCloseHdr> findByTransactionPoidAndGroupPoid(Long id, Long groupPoid);

    @Query("""
       SELECT h 
       FROM ArShDayEndCloseHdr h
       WHERE h.transactionPoid = :id
       AND (h.deleted = 'N' OR h.deleted IS NULL)
       """)
    Optional<ArShDayEndCloseHdr> findByTransactionPoidDeleted(Long id);
}
