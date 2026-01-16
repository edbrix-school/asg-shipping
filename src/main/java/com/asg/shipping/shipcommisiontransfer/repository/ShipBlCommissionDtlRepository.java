package com.asg.shipping.shipcommisiontransfer.repository;

import com.asg.shipping.shipcommisiontransfer.entity.ShipBlCommissionDtl;
import com.asg.shipping.shipcommisiontransfer.entity.ShipBlCommissionDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ShipBlCommissionDtl entity
 */
@Repository
public interface ShipBlCommissionDtlRepository extends JpaRepository<ShipBlCommissionDtl, ShipBlCommissionDtlId> {

    List<ShipBlCommissionDtl> findByTransactionPoidOrderByDetRowId(Long transactionPoid);

    Optional<ShipBlCommissionDtl> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);

    void deleteByTransactionPoid(Long transactionPoid);

    @Query("SELECT COALESCE(MAX(d.detRowId), 0) FROM ShipBlCommissionDtl d WHERE d.transactionPoid = :transactionPoid")
    Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);

    @Query("SELECT COUNT(d) > 0 FROM ShipBlCommissionDtl d WHERE d.transactionPoid = :transactionPoid AND d.selected = 'Y'")
    boolean hasSelectedDetails(@Param("transactionPoid") Long transactionPoid);
}
