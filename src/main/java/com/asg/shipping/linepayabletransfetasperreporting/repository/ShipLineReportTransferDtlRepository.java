package com.asg.shipping.linepayabletransfetasperreporting.repository;


import com.asg.shipping.linepayabletransfetasperreporting.entity.ShipLineReportTransferDtl;
import com.asg.shipping.linepayabletransfetasperreporting.entity.ShipLineReportTransferDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for SHIP_LINE_REPORT_TRANSFER_DTL
 */
@Repository
public interface ShipLineReportTransferDtlRepository extends JpaRepository<ShipLineReportTransferDtl, ShipLineReportTransferDtlId> {

    @Query("SELECT d FROM ShipLineReportTransferDtl d WHERE d.transactionPoid = :transactionPoid " +
            "ORDER BY d.detRowId")
    List<ShipLineReportTransferDtl> findByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    @Modifying
    @Query("DELETE FROM ShipLineReportTransferDtl d WHERE d.transactionPoid = :transactionPoid")
    void deleteByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    @Query("SELECT COALESCE(MAX(d.detRowId), 0) FROM ShipLineReportTransferDtl d WHERE d.transactionPoid = :transactionPoid")
    Long findMaxDetRowIdByTransactionPoid(@Param("transactionPoid") Long transactionPoid);
}
