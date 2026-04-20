package com.asg.shipping.bookingFormSH.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.asg.shipping.bookingFormSH.entity.ShipMateChargesDtl;
import com.asg.shipping.bookingFormSH.entity.ShipMateChargesDtlId;

/**
 * Repository for ShipMateChargesDtl entity
 */
@Repository
public interface ShipMateChargesDtlRepository extends JpaRepository<ShipMateChargesDtl, ShipMateChargesDtlId> {

	List<ShipMateChargesDtl> findByTransactionPoidOrderByDetRowId(Long transactionPoid);

	Optional<ShipMateChargesDtl> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);

	void deleteByTransactionPoid(Long transactionPoid);

    void deleteByTransactionPoidAndDetRowIdIn(Long transactionPoid, List<Long> detRowIds);

	@Query("SELECT COALESCE(MAX(d.detRowId), 0) FROM ShipMateChargesDtl d WHERE d.transactionPoid = :transactionPoid")
	Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);
}
