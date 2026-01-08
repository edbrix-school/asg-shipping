package com.asg.shipping.salesinvoice.repository;

import com.asg.shipping.salesinvoice.entity.ArShSalesInvoiceHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for AR_SH_SALES_INVOICE_HDR
 */
@Repository
public interface ArShSalesInvoiceHdrRepository extends JpaRepository<ArShSalesInvoiceHdr, Long> {

    @Query("SELECT h FROM ArShSalesInvoiceHdr h WHERE h.transactionPoid = :transactionPoid "
//           Need to use this if deleted records need not be fetched
//            +
//           "AND (h.deleted IS NULL OR h.deleted = 'N')"
    )
    Optional<ArShSalesInvoiceHdr> findActiveByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    @Query("SELECT h FROM ArShSalesInvoiceHdr h WHERE h.docRef = :docRef AND h.invDate = :invDate " +
           "AND (h.deleted IS NULL OR h.deleted = 'N')")
    Optional<ArShSalesInvoiceHdr> findByDocRefAndInvDate(
            @Param("docRef") String docRef,
            @Param("invDate") LocalDate invDate);

    @Query("SELECT h FROM ArShSalesInvoiceHdr h WHERE h.blPoid = :blPoid " +
           "AND (h.deleted IS NULL OR h.deleted = 'N')")
    List<ArShSalesInvoiceHdr> findByBlPoid(@Param("blPoid") Long blPoid);

    @Query("SELECT COUNT(h) > 0 FROM ArShSalesInvoiceHdr h WHERE h.docRef = :docRef " +
           "AND h.invDate = :invDate AND (h.deleted IS NULL OR h.deleted = 'N') " +
           "AND (:excludeTransactionPoid IS NULL OR h.transactionPoid <> :excludeTransactionPoid)")
    boolean existsByDocRefAndInvDate(
            @Param("docRef") String docRef,
            @Param("invDate") LocalDate invDate,
            @Param("excludeTransactionPoid") Long excludeTransactionPoid);
}

