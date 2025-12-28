package com.asg.shipping.linecommission.repository;

import com.asg.shipping.linecommission.entity.ShipLineCommDtlEntity;
import com.asg.shipping.linecommission.entity.ShipLineCommDtlId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShipLineCommDtlRepository extends JpaRepository<ShipLineCommDtlEntity, ShipLineCommDtlId> {
    List<ShipLineCommDtlEntity> findByTransactionPoidOrderByDetRowId(Long transactionPoid);
    void deleteByTransactionPoid(Long transactionPoid);
    Optional<ShipLineCommDtlEntity> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);
}


