package com.asg.shipping.collectionhandover.repository;

import com.asg.shipping.collectionhandover.entity.ArShDayEndCloseDtl;
import com.asg.shipping.collectionhandover.entity.ArShDayEndCloseDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ArShDayEndCloseDtl entity in Collection Handover context
 */
@Repository
public interface CollectionHandoverDtlRepository extends JpaRepository<ArShDayEndCloseDtl, ArShDayEndCloseDtlId> {

    /**
     * Find all detail records for a transaction, ordered by DET_ROW_ID
     */
    List<ArShDayEndCloseDtl> findByTransactionPoidOrderByDetRowId(Long transactionPoid);

    /**
     * Find detail record by transaction and det row id
     */
    Optional<ArShDayEndCloseDtl> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);

    /**
     * Delete all detail records for a transaction
     */
    void deleteByTransactionPoid(Long transactionPoid);

    /**
     * Get max DET_ROW_ID for a transaction (for generating new DET_ROW_ID)
     */
    @Query("SELECT COALESCE(MAX(d.detRowId), 0) FROM CollectionHandoverDtl d WHERE d.transactionPoid = :transactionPoid")
    Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);
}