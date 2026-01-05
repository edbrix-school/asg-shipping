package com.asg.shipping.dayCloseShiping.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.asg.shipping.dayCloseShiping.entity.ArShDayEndCloseHdr;

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
}
