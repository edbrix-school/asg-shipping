package com.asg.shipping.containertypeportchargestariff.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public class PortChargesTariffCustomRepositoryImpl implements PortChargesTariffCustomRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void copyPortChargesTariff(Long sourceTransactionPoid) {

        StoredProcedureQuery query = em.createStoredProcedureQuery("COPY_PORT_CHARGE");

        query.registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN);
        query.setParameter("P_TRANSACTION_POID", sourceTransactionPoid);

        query.execute();
    }
}