package com.asg.shipping.common.repository;

import com.asg.shipping.common.entity.GlobalCurrencyMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface GlobalCurrencyMasterRepository extends JpaRepository<GlobalCurrencyMaster, BigDecimal> {

    Optional<GlobalCurrencyMaster> findByCurrencyPoid(BigDecimal currencyPoid);

    List<GlobalCurrencyMaster> findByCurrencyCodeIgnoreCaseIn(List<String> currencyCodes);
}

