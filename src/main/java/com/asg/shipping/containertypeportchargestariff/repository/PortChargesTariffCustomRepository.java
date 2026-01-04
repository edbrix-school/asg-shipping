package com.asg.shipping.containertypeportchargestariff.repository;

public interface PortChargesTariffCustomRepository {
    void copyPortChargesTariff(Long sourceTransactionPoid);
}