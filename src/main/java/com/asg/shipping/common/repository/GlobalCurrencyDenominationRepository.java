package com.asg.shipping.common.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.asg.shipping.common.entity.GlobalCurrencyDenomination;

@Repository
public interface GlobalCurrencyDenominationRepository extends JpaRepository<GlobalCurrencyDenomination, Long> {

	List<GlobalCurrencyDenomination> findByCurrencyCodeOrderBySeqNo(String currencyCode);
}
