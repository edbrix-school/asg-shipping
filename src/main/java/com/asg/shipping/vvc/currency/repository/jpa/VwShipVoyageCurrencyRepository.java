package com.asg.shipping.vvc.currency.repository.jpa;

import com.asg.shipping.vvc.currency.entity.VwShipVoyageCurrencyEntity;
import com.asg.shipping.vvc.currency.entity.VwShipVoyageCurrencyId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VwShipVoyageCurrencyRepository extends JpaRepository<VwShipVoyageCurrencyEntity, VwShipVoyageCurrencyId> {
    List<VwShipVoyageCurrencyEntity> findByTransactionPoidOrderByDetRowIdAsc(Long transactionPoid);
}


