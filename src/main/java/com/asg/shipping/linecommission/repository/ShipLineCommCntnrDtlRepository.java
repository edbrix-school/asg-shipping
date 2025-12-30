package com.asg.shipping.linecommission.repository;

import com.asg.shipping.linecommission.entity.ShipLineCommCntnrDtlEntity;
import com.asg.shipping.linecommission.entity.ShipLineCommCntnrDtlId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShipLineCommCntnrDtlRepository extends JpaRepository<ShipLineCommCntnrDtlEntity, ShipLineCommCntnrDtlId> {
    List<ShipLineCommCntnrDtlEntity> findByTransactionPoidOrderByDetRowId(Long transactionPoid);
    void deleteByTransactionPoid(Long transactionPoid);
    Optional<ShipLineCommCntnrDtlEntity> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);
}


