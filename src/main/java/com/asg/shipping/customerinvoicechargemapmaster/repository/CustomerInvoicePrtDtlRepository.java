package com.asg.shipping.customerinvoicechargemapmaster.repository;

import com.asg.shipping.customerinvoicechargemapmaster.entity.CustomerInvoicePrtDtlEntity;
import com.asg.shipping.customerinvoicechargemapmaster.entity.CustomerInvoicePrtDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerInvoicePrtDtlRepository
        extends JpaRepository<CustomerInvoicePrtDtlEntity, CustomerInvoicePrtDtlId> {

    List<CustomerInvoicePrtDtlEntity> findByIdCustomerPoid(Long customerPoid);

    @Query("SELECT COALESCE(MAX(d.id.detRowId), 0) FROM CustomerInvoicePrtDtlEntity d WHERE d.id.customerPoid = :customerPoid")
    Long findMaxDetRowId(@Param("customerPoid") Long customerPoid);
}
