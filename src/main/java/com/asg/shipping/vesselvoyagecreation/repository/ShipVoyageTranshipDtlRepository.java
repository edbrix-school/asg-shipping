package com.asg.shipping.vesselvoyagecreation.repository;

import com.asg.shipping.vesselvoyagecreation.entity.ShipVoyageTranshipDtlEntity;
import com.asg.shipping.vesselvoyagecreation.entity.ShipVoyageTranshipDtlId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShipVoyageTranshipDtlRepository extends JpaRepository<ShipVoyageTranshipDtlEntity, ShipVoyageTranshipDtlId> {
    List<ShipVoyageTranshipDtlEntity> findByTransactionPoidOrderByDetRowIdAsc(Long transactionPoid);
}










