package com.asg.shipping.salesinvoice.repository;

import com.asg.shipping.salesinvoice.entity.ArShSalesInvoiceChargDtl;
import com.asg.shipping.salesinvoice.entity.ArShSalesInvoiceChargDtlId;
import com.asg.shipping.salesinvoice.entity.ArShSalesInvoiceContnrDtl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository for AR_SH_SALES_INVOICE_CHARG_DTL
 */
@Repository
public interface ArShSalesInvoiceChargDtlRepository extends JpaRepository<ArShSalesInvoiceChargDtl, ArShSalesInvoiceChargDtlId> {

    @Query("SELECT d FROM ArShSalesInvoiceChargDtl d WHERE d.transactionPoid = :transactionPoid " +
           "ORDER BY d.detRowId")
    List<ArShSalesInvoiceChargDtl> findByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    @Query("SELECT d FROM ArShSalesInvoiceChargDtl d WHERE d.blPoid = :blPoid " +
           "AND d.transactionPoid = :transactionPoid ORDER BY d.detRowId")
    List<ArShSalesInvoiceChargDtl> findByBlPoidAndTransactionPoid(
            @Param("blPoid") Long blPoid,
            @Param("transactionPoid") Long transactionPoid);

    @Query("SELECT d FROM ArShSalesInvoiceChargDtl d WHERE d.chargePoid = :chargePoid " +
           "AND d.transactionPoid = :transactionPoid AND d.amountSelect = 'Y'")
    List<ArShSalesInvoiceChargDtl> findByChargePoidAndTransactionPoid(
            @Param("chargePoid") Long chargePoid,
            @Param("transactionPoid") Long transactionPoid);

    @Query("SELECT SUM(d.amount) FROM ArShSalesInvoiceChargDtl d WHERE d.transactionPoid = :transactionPoid " +
           "AND d.amountSelect = 'Y' AND d.chargePoid = :chargePoid")
    BigDecimal sumAmountByTransactionPoidAndChargePoid(
            @Param("transactionPoid") Long transactionPoid,
            @Param("chargePoid") Long chargePoid);

    @Query("SELECT SUM(d.amount) FROM ArShSalesInvoiceChargDtl d WHERE d.transactionPoid = :transactionPoid " +
           "AND d.amountSelect = 'Y' AND d.chargeType = 'MANIFEST'")
    BigDecimal sumManifestChargeAmountByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    @Modifying
    @Query("DELETE FROM ArShSalesInvoiceChargDtl d WHERE d.transactionPoid = :transactionPoid")
    void deleteByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    @Query("SELECT MAX(d.detRowId) FROM ArShSalesInvoiceChargDtl d WHERE d.transactionPoid = :transactionPoid")
    Long findMaxDetRowIdByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    List<ArShSalesInvoiceChargDtl> findByTransactionPoidOrderByDetRowId(Long transactionPoid);

    void deleteByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);

    Optional<ArShSalesInvoiceChargDtl> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);
}

