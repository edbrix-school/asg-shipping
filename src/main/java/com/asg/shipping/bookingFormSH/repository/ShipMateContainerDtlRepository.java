package com.asg.shipping.bookingFormSH.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.asg.shipping.bookingFormSH.entity.ShipMateContainerDtl;
import com.asg.shipping.bookingFormSH.entity.ShipMateContainerDtlId;

/**
 * Repository for ShipMateContainerDtl entity
 */
@Repository
public interface ShipMateContainerDtlRepository extends JpaRepository<ShipMateContainerDtl, ShipMateContainerDtlId> {

	List<ShipMateContainerDtl> findByTransactionPoidOrderByDetRowId(Long transactionPoid);

	Optional<ShipMateContainerDtl> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);

	Optional<ShipMateContainerDtl> findByTransactionPoidAndContainerNo(Long transactionPoid, String containerNo);

	void deleteByTransactionPoid(Long transactionPoid);

	@Query("SELECT COALESCE(MAX(d.detRowId), 0) FROM ShipMateContainerDtl d WHERE d.transactionPoid = :transactionPoid")
	Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);

	@Query("SELECT COUNT(d) > 0 FROM ShipMateContainerDtl d WHERE d.transactionPoid = :transactionPoid AND d.containerNo IS NOT NULL AND d.returnFromShipper IS NULL")
	boolean existsByTransactionPoidWithContainerAndNoReturn(@Param("transactionPoid") Long transactionPoid);
}