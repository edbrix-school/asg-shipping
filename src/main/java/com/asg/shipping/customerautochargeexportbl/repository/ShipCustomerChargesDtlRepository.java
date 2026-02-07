package com.asg.shipping.customerautochargeexportbl.repository;

import com.asg.shipping.customerautochargeexportbl.entity.CustomerChargesDetailId;
import com.asg.shipping.customerautochargeexportbl.entity.ShipCustomerChargesDtlEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ShipCustomerChargesDtlRepository extends JpaRepository<ShipCustomerChargesDtlEntity, CustomerChargesDetailId> {

    List<ShipCustomerChargesDtlEntity> findByTransactionPoidOrderByDetRowId(Long transactionPoid);

    void deleteByTransactionPoid(Long transactionPoid);
    
    void deleteByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);
    
    Optional<ShipCustomerChargesDtlEntity> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);

    @Query("SELECT COALESCE(MAX(d.detRowId), 0) FROM ShipCustomerChargesDtlEntity d WHERE d.transactionPoid = :transactionPoid")
    Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);
}

