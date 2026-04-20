package com.asg.shipping.bookingFormSH.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.asg.shipping.bookingFormSH.entity.ShipMateCargoDtl;
import com.asg.shipping.bookingFormSH.entity.ShipMateCargoDtlId;

/**
 * Repository for ShipMateCargoDtl entity
 */
@Repository
public interface ShipMateCargoDtlRepository extends JpaRepository<ShipMateCargoDtl, ShipMateCargoDtlId> {

	List<ShipMateCargoDtl> findByTransactionPoidOrderByDetRowId(Long transactionPoid);

	Optional<ShipMateCargoDtl> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);

	void deleteByTransactionPoid(Long transactionPoid);

    void deleteByTransactionPoidAndDetRowIdIn( Long transactionPoid, List<Long> detRowIds);

	@Query("SELECT COALESCE(MAX(d.detRowId), 0) FROM ShipMateCargoDtl d WHERE d.transactionPoid = :transactionPoid")
	Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);
}