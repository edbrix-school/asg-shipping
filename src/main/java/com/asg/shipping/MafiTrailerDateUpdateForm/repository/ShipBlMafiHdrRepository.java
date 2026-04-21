package com.asg.shipping.MafiTrailerDateUpdateForm.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.asg.shipping.MafiTrailerDateUpdateForm.entity.ShipBlMafiHdr;

@Repository
public interface ShipBlMafiHdrRepository extends JpaRepository<ShipBlMafiHdr, Long> {

	@Query(value = """
			SELECT
			    h.TRANSACTION_POID AS transactionPoid,
			    h.TRANSACTION_DATE AS transactionDate,
			    h.DOC_REF AS docRef,
			    h.VOYAGE_TRANSACTION_POID AS voyageTransactionPoid,
			    v.VOYAGE_NO AS voyageNo,
			    vs.VESSEL_NAME AS vesselName,
			    h.AGENT_REFERENCE AS agentReference,
			    h.REMARKS AS remarks,
			    h.DELETED AS deleted,
			    h.CREATED_BY AS createdBy,
			    h.CREATED_DATE AS createdDate,
			    h.LASTMODIFIED_BY AS lastModifiedBy,
			    h.LASTMODIFIED_DATE AS lastModifiedDate
			FROM SHIP_BL_MAFI_HDR h
			LEFT JOIN SHIP_VOYAGE_HDR v ON v.TRANSACTION_POID = h.VOYAGE_TRANSACTION_POID
			LEFT JOIN SHIP_VESSEL_MASTER vs ON vs.VESSEL_POID = v.VESSEL_POID
			WHERE h.GROUP_POID = :groupPoid
			  AND h.COMPANY_POID = :companyPoid
			  AND (:deleted IS NULL OR h.DELETED = :deleted)
			  AND (:docRef IS NULL OR UPPER(h.DOC_REF) LIKE UPPER(CONCAT('%', :docRef, '%')))
			  AND (:voyageTransactionPoid IS NULL OR h.VOYAGE_TRANSACTION_POID = :voyageTransactionPoid)
			  AND (:agentReference IS NULL OR UPPER(h.AGENT_REFERENCE) LIKE UPPER(CONCAT('%', :agentReference, '%')))
			  AND (:transactionDateFrom IS NULL OR h.TRANSACTION_DATE >= :transactionDateFrom)
			  AND (:transactionDateTo IS NULL OR h.TRANSACTION_DATE <= :transactionDateTo)
			""", countQuery = """
			SELECT COUNT(*)
			FROM SHIP_BL_MAFI_HDR h
			LEFT JOIN SHIP_VOYAGE_HDR v ON v.TRANSACTION_POID = h.VOYAGE_TRANSACTION_POID
			LEFT JOIN SHIP_VESSEL_MASTER vs ON vs.VESSEL_POID = v.VESSEL_POID
			WHERE h.GROUP_POID = :groupPoid
			  AND h.COMPANY_POID = :companyPoid
			  AND (:deleted IS NULL OR h.DELETED = :deleted)
			  AND (:docRef IS NULL OR UPPER(h.DOC_REF) LIKE UPPER(CONCAT('%', :docRef, '%')))
			  AND (:voyageTransactionPoid IS NULL OR h.VOYAGE_TRANSACTION_POID = :voyageTransactionPoid)
			  AND (:agentReference IS NULL OR UPPER(h.AGENT_REFERENCE) LIKE UPPER(CONCAT('%', :agentReference, '%')))
			  AND (:transactionDateFrom IS NULL OR h.TRANSACTION_DATE >= :transactionDateFrom)
			  AND (:transactionDateTo IS NULL OR h.TRANSACTION_DATE <= :transactionDateTo)
			""", nativeQuery = true)
	Page<Object> searchMafiTrailers(@Param("groupPoid") Long groupPoid, @Param("companyPoid") Long companyPoid,
			@Param("deleted") String deleted, @Param("docRef") String docRef,
			@Param("voyageTransactionPoid") Long voyageTransactionPoid, @Param("agentReference") String agentReference,
			@Param("transactionDateFrom") LocalDate transactionDateFrom,
			@Param("transactionDateTo") LocalDate transactionDateTo, Pageable pageable);

	Optional<ShipBlMafiHdr> findByTransactionPoidAndDeleted(Long transactionPoid, String deleted);
	
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
	    UPDATE SHIP_BL_MAFI_HDR
	    SET AGENT_REFERENCE = :agentReference,
	        REMARKS = :remarks,
	        TRANSACTION_DATE = :transactionDate
	        LASTMODIFIED_BY = :userId,
	        LASTMODIFIED_DATE = CURRENT_TIMESTAMP
	    WHERE TRANSACTION_POID = :transactionPoid
	""", nativeQuery = true)
	int updateByTransactionPoid(
	        @Param("transactionPoid") Long transactionPoid,
			@Param("transactionDate") LocalDate transactionDate,
	        @Param("agentReference") String agentReference,
	        @Param("remarks") String remarks,
	        @Param("userId") String userId
	);

}
