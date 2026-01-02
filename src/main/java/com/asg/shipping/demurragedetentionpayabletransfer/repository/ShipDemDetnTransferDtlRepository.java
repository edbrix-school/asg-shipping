package com.asg.shipping.demurragedetentionpayabletransfer.repository;

import com.asg.shipping.demurragedetentionpayabletransfer.entity.ShipDemDetnTransferDtl;
import com.asg.shipping.demurragedetentionpayabletransfer.entity.ShipDemDetnTransferDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ShipDemDetnTransferDtl entity
 */
@Repository
public interface ShipDemDetnTransferDtlRepository extends JpaRepository<ShipDemDetnTransferDtl, ShipDemDetnTransferDtlId> {

    List<ShipDemDetnTransferDtl> findByTransactionPoidOrderByDetRowId(Long transactionPoid);

    Optional<ShipDemDetnTransferDtl> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);

    void deleteByTransactionPoid(Long transactionPoid);

    @Query("SELECT COALESCE(MAX(d.detRowId), 0) FROM ShipDemDetnTransferDtl d WHERE d.transactionPoid = :transactionPoid")
    Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);

    @Query("SELECT COUNT(d) > 0 FROM ShipDemDetnTransferDtl d " +
            "WHERE d.mainfestTransactionPoid = :mainfestTransactionPoid " +
            "AND d.containerNo = :containerNo " +
            "AND d.isSelect = 'Y' " +
            "AND d.shipDemDetnTransferHdr.deleted != 'Y'")
    boolean existsByManifestAndContainerAndSelected(@Param("mainfestTransactionPoid") Long mainfestTransactionPoid,
                                                    @Param("containerNo") String containerNo);
}
