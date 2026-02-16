package com.asg.shipping.deliveryorderissuetocustomer.repository;

import com.asg.shipping.deliveryorderissuetocustomer.entity.DoShPrintingDtl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoShPrintingDtlRepository extends JpaRepository<DoShPrintingDtl, Long> {
    Optional<DoShPrintingDtl> findByTransactionPoid(Long transactionPoid);
}
