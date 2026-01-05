package com.asg.shipping.dayCloseShiping.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.asg.shipping.dayCloseShiping.entity.ArShDayEndCloseDtl;
import com.asg.shipping.dayCloseShiping.entity.ArShDayEndCloseDtlId;

@Repository
public interface ArShDayEndCloseDtlRepository extends JpaRepository<ArShDayEndCloseDtl, ArShDayEndCloseDtlId> {

	List<ArShDayEndCloseDtl> findByTransactionPoid(Long transactionPoid);
	
	@Query("SELECT COALESCE(MAX(d.detRowId), 0) FROM ArShDayEndCloseDtl d WHERE d.transactionPoid = :transactionPoid")
    Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);
}
