package com.asg.shipping.demurragedetentionpayabletransfer.repository;

import com.asg.shipping.demurragedetentionpayabletransfer.entity.ShipDemDtnTransferBillDtl;
import com.asg.shipping.demurragedetentionpayabletransfer.entity.ShipDemDtnTransferBillDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ShipDemDtnTransferBillDtl entity
 */
@Repository
public interface ShipDemDtnTransferBillDtlRepository extends JpaRepository<ShipDemDtnTransferBillDtl, ShipDemDtnTransferBillDtlId> {

    List<ShipDemDtnTransferBillDtl> findByTransactionPoidOrderByDetRowId(Long transactionPoid);

    Optional<ShipDemDtnTransferBillDtl> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);

    void deleteByTransactionPoid(Long transactionPoid);

    @Query("SELECT COALESCE(MAX(d.detRowId), 0) FROM ShipDemDtnTransferBillDtl d WHERE d.transactionPoid = :transactionPoid")
    Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);
}
