package com.asg.shipping.exportManifestBl.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oracle.jdbc.OracleTypes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Slf4j
@RequiredArgsConstructor
public class ExportManifestBlProcRepositoryImpl implements ExportManifestBlProcRepository {

    private static final String UPDATE_TYPE_AUTOSUM_WEIGHT_PACK = "AUTOSUMWEIGHTPACKATE";
    /** Legacy ADF: BEGIN PROC_SHIP_BL_DAMAGE_LOAD(?); END; (no schema prefix, no IN args). */
    private static final String DAMAGE_LOAD_SQL = "BEGIN PROC_SHIP_BL_DAMAGE_LOAD(?); END;";

    @PersistenceContext
    private EntityManager entityManager;

    private final JdbcTemplate jdbcTemplate;

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
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void processExportLocalCharge(Long groupPoid, Long companyPoid, Long transactionPoid) {
        String sql = "{call PROC_SHIP_BL_PAGE_SAVE_AFTER(?, ?, ?, ?, ?)}";
        jdbcTemplate.execute(sql, (CallableStatement cs) -> {
            cs.setLong(1, groupPoid);
            cs.setLong(2, companyPoid);
            cs.setLong(3, transactionPoid);
            cs.setNull(4, Types.NUMERIC);
            cs.setString(5, UPDATE_TYPE_AUTOSUM_WEIGHT_PACK);
            cs.execute();
            return null;
        });
        log.debug(
                "Legacy exportLocalCharge proc executed for transactionPoid {} with {}",
                transactionPoid,
                UPDATE_TYPE_AUTOSUM_WEIGHT_PACK);
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

    @Override
    @Transactional(readOnly = true)
    public List<String> loadDamageClauseLines() {
        log.debug("Calling legacy damage load: {}", DAMAGE_LOAD_SQL);

        return jdbcTemplate.execute((Connection con) -> {
            try (CallableStatement cs = con.prepareCall(DAMAGE_LOAD_SQL)) {
                cs.registerOutParameter(1, OracleTypes.CURSOR);
                cs.execute();

                List<String> lines = new ArrayList<>();
                try (ResultSet rs = (ResultSet) cs.getObject(1)) {
                    if (rs != null) {
                        while (rs.next()) {
                            String clause = rs.getString("DAMAGE_CLAUSE");
                            if (clause != null && !clause.isBlank()) {
                                lines.add(clause.trim());
                            }
                        }
                    }
                }
                return lines;
            }
        });
    }
}
