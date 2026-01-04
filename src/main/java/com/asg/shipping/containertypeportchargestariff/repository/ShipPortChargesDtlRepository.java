package com.asg.shipping.containertypeportchargestariff.repository;

import com.asg.shipping.containertypeportchargestariff.entity.ShipPortChargesDtl;
import com.asg.shipping.containertypeportchargestariff.entity.ShipPortChargesDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipPortChargesDtlRepository extends JpaRepository<ShipPortChargesDtl, ShipPortChargesDtlId> {
    
    List<ShipPortChargesDtl> findByTransactionPoid(Long transactionPoid);
    
    Optional<ShipPortChargesDtl> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);
    
    @Modifying
    @Query("DELETE FROM ShipPortChargesDtl d WHERE d.transactionPoid = :transactionPoid")
    void deleteByTransactionPoid(@Param("transactionPoid") Long transactionPoid);
}
