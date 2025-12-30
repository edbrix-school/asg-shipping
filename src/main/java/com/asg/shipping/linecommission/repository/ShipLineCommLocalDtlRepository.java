package com.asg.shipping.linecommission.repository;

import com.asg.shipping.linecommission.entity.ShipLineCommLocalDtlEntity;
import com.asg.shipping.linecommission.entity.ShipLineCommLocalDtlId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShipLineCommLocalDtlRepository extends JpaRepository<ShipLineCommLocalDtlEntity, ShipLineCommLocalDtlId> {
    List<ShipLineCommLocalDtlEntity> findByTransactionPoidOrderByDetRowId(Long transactionPoid);
    void deleteByTransactionPoid(Long transactionPoid);
    Optional<ShipLineCommLocalDtlEntity> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);
}


