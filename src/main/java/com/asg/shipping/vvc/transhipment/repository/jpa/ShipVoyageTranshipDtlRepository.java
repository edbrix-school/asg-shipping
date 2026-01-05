package com.asg.shipping.vvc.transhipment.repository.jpa;

import com.asg.shipping.vvc.transhipment.entity.ShipVoyageTranshipDtlEntity;
import com.asg.shipping.vvc.transhipment.entity.ShipVoyageTranshipDtlId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShipVoyageTranshipDtlRepository extends JpaRepository<ShipVoyageTranshipDtlEntity, ShipVoyageTranshipDtlId> {
    List<ShipVoyageTranshipDtlEntity> findByTransactionPoidOrderByDetRowIdAsc(Long transactionPoid);
}


