package com.asg.shipping.customerinvoicechargemapmaster.repository;

import com.asg.shipping.customerinvoicechargemapmaster.entity.CustomerInvoicePrtMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerInvoicePrtMasterRepository
        extends JpaRepository<CustomerInvoicePrtMasterEntity, Long> {
}

