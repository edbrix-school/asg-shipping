package com.asg.shipping.salesinvoice.repository;

import com.asg.shipping.salesinvoice.entity.ArShSalesInvoiceContnrDtl;
import com.asg.shipping.salesinvoice.entity.ArShSalesInvoiceContnrDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for AR_SH_SALES_INVOICE_CONTNR_DTL
 */
@Repository
public interface ArShSalesInvoiceContnrDtlRepository extends JpaRepository<ArShSalesInvoiceContnrDtl, ArShSalesInvoiceContnrDtlId> {

    @Query("SELECT d FROM ArShSalesInvoiceContnrDtl d WHERE d.transactionPoid = :transactionPoid " +
           "ORDER BY d.detRowId")
    List<ArShSalesInvoiceContnrDtl> findByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    @Query("SELECT d FROM ArShSalesInvoiceContnrDtl d WHERE d.blPoid = :blPoid " +
           "AND d.transactionPoid = :transactionPoid ORDER BY d.detRowId")
    List<ArShSalesInvoiceContnrDtl> findByBlPoidAndTransactionPoid(
            @Param("blPoid") Long blPoid,
            @Param("transactionPoid") Long transactionPoid);

    @Modifying
    @Query("DELETE FROM ArShSalesInvoiceContnrDtl d WHERE d.transactionPoid = :transactionPoid")
    void deleteByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    @Query("SELECT MAX(d.detRowId) FROM ArShSalesInvoiceContnrDtl d WHERE d.transactionPoid = :transactionPoid")
    Long findMaxDetRowIdByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    List<ArShSalesInvoiceContnrDtl> findByTransactionPoidOrderByDetRowId(Long transactionPoid);
}

