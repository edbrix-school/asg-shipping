package com.asg.shipping.common.repository;


import com.asg.shipping.common.entity.GlobalCurrencyRates;
import com.asg.shipping.common.entity.GlobalCurrencyRatesId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GlobalCurrencyRatesRepository extends JpaRepository<GlobalCurrencyRates, GlobalCurrencyRatesId> {

    @Query("SELECT r FROM GlobalCurrencyRates r WHERE r.currencyCode = :currencyCode AND (r.deleted IS NULL OR r.deleted != 'Y') ORDER BY r.rateDate DESC")
    Optional<GlobalCurrencyRates> findLatestByCurrencyCode(@Param("currencyCode") String currencyCode);
}

