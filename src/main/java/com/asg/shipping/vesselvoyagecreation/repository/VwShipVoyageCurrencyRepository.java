package com.asg.shipping.vesselvoyagecreation.repository;

import com.asg.shipping.vesselvoyagecreation.entity.VwShipVoyageCurrencyEntity;
import com.asg.shipping.vesselvoyagecreation.entity.VwShipVoyageCurrencyId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VwShipVoyageCurrencyRepository extends JpaRepository<VwShipVoyageCurrencyEntity, VwShipVoyageCurrencyId> {
    List<VwShipVoyageCurrencyEntity> findByTransactionPoidOrderByDetRowIdAsc(Long transactionPoid);
}










