package com.asg.shipping.exportManifestBl.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Slf4j
public class ExportManifestBlProcRepositoryImpl implements ExportManifestBlProcRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Object[] getTaxRate(Long chargePoid, Long companyPoid, LocalDate transactionDate) {
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("get_tax_rate");

            query.registerStoredProcedureParameter("P_CHARGE_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_TAX_POID", Long.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("P_TAX_PERCENTAGE", BigDecimal.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_TRANSACTION_DATE", Date.class, ParameterMode.IN);

            query.setParameter("P_CHARGE_POID", chargePoid);
            query.setParameter("P_COMPANY_POID", companyPoid);
            query.setParameter("P_TRANSACTION_DATE",
                    transactionDate != null ? Date.valueOf(transactionDate) : null);

            query.execute();

            Long taxPoid = (Long) query.getOutputParameterValue("P_TAX_POID");
            BigDecimal taxPercentage = (BigDecimal) query.getOutputParameterValue("P_TAX_PERCENTAGE");
            return new Object[]{taxPoid, taxPercentage};
        } catch (Exception e) {
            log.error("Error calling get_tax_rate procedure for chargePoid: {}", chargePoid, e);
            return new Object[]{null, BigDecimal.ZERO};
        }
    }

    @Override
    @Transactional
    public void processAfterSave(Long groupPoid, Long companyPoid, Long transactionPoid, Long detRowId,
                                 String updateType, Long userPoid) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_SHIP_BL_PAGE_SAVE_AFTER");

        query.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DET_ROW_ID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_UPDATE_TYPE", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER", Long.class, ParameterMode.IN);

        query.setParameter("P_GROUP_POID", groupPoid);
        query.setParameter("P_COMPANY_POID", companyPoid);
        query.setParameter("P_DOC_KEY_POID", transactionPoid);
        query.setParameter("P_DET_ROW_ID", detRowId);
        query.setParameter("P_UPDATE_TYPE", updateType);
        query.setParameter("P_LOGIN_USER", userPoid);

        query.execute();
    }

    @Override
    @Transactional
    public void loadCustomerAutoCharges(Long groupPoid, Long companyPoid, Long transactionPoid, Long detRowId,
                                        String updateType, Long userPoid) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_SHIP_BL_CUSTOMER_AUTO");

        query.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DET_ROW_ID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_UPDATE_TYPE", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER", String.class, ParameterMode.IN);

        query.setParameter("P_GROUP_POID", groupPoid);
        query.setParameter("P_COMPANY_POID", companyPoid);
        query.setParameter("P_DOC_KEY_POID", transactionPoid);
        query.setParameter("P_DET_ROW_ID", detRowId);
        query.setParameter("P_UPDATE_TYPE", updateType);
        query.setParameter("P_LOGIN_USER", userPoid != null ? String.valueOf(userPoid) : null);

        query.execute();
    }
}
