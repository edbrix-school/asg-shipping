package com.asg.shipping.bookingFormSH.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.asg.shipping.bookingFormSH.entity.ShipMateHdr;

/**
 * Repository for ShipMateHdr entity
 */
@Repository
public interface ShipMateHdrRepository extends JpaRepository<ShipMateHdr, Long> {

	Optional<ShipMateHdr> findByTransactionPoid(Long transactionPoid);

	Optional<ShipMateHdr> findByDocRefAndDeletedNot(String docRef, String deleted);

	Optional<ShipMateHdr> findByBookingIssueNoAndDeletedNot(String bookingIssueNo, String deleted);

	@Query("SELECT COUNT(h) > 0 FROM ShipMateHdr h WHERE h.docRef = :docRef AND h.deleted != 'Y'")
	boolean existsByDocRefAndNotDeleted(@Param("docRef") String docRef);

	@Query("SELECT COUNT(h) > 0 FROM ShipMateHdr h WHERE h.bookingIssueNo = :bookingIssueNo AND h.deleted != 'Y'")
	boolean existsByBookingIssueNoAndNotDeleted(@Param("bookingIssueNo") String bookingIssueNo);

	@Query("SELECT COUNT(h) > 0 FROM ShipMateHdr h WHERE h.bookingIssueNo = :bookingIssueNo AND h.deleted != 'Y' AND h.transactionPoid != :excludeTransactionPoid")
	boolean existsByBookingIssueNoAndNotDeletedExcludingPoid(@Param("bookingIssueNo") String bookingIssueNo,
			@Param("excludeTransactionPoid") Long excludeTransactionPoid);
}
