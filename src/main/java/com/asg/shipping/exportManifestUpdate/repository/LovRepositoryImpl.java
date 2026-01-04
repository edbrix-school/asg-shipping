package com.asg.shipping.exportManifestUpdate.repository;

import com.asg.shipping.exportManifestUpdate.dto.LovItem;
import com.asg.shipping.exportManifestUpdate.dto.LovResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of LOV Repository that calls PROC_LOV_GETLIST stored procedure
 */
@Repository
@Slf4j
public class LovRepositoryImpl implements LovRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public LovResponse getLovList(String lovName, Long docKeyPoid, String filterField, String filterValue,
                                  Long groupPoid, Long companyPoid, Long userPoid) {
        log.debug("Fetching LOV list: lovName={}, docKeyPoid={}, filterField={}, filterValue={}, groupPoid={}, companyPoid={}, userPoid={}",
                lovName, docKeyPoid, filterField, filterValue, groupPoid, companyPoid, userPoid);

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_LOV_GETLIST");

        // Register parameters
        query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOV_NAME", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOV_FILTER_FIELD", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOV_FILTER_VALUE", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);

        // Set parameters
        query.setParameter("P_LOGIN_GROUP_POID", groupPoid);
        query.setParameter("P_LOGIN_COMPANY_POID", companyPoid);
        query.setParameter("P_LOGIN_USER_POID", userPoid);
        query.setParameter("P_LOV_NAME", lovName);
        query.setParameter("P_LOV_FILTER_FIELD", filterField != null ? filterField : "");
        query.setParameter("P_LOV_FILTER_VALUE", filterValue != null ? filterValue : "");

        // Execute and get results
        query.execute();

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<LovItem> items = new ArrayList<>();
        if (results != null) {
            for (Object[] row : results) {
                LovItem item = new LovItem();
                // POID is first column
                if (row[0] != null) {
                    if (row[0] instanceof BigDecimal) {
                        item.setPoid(((BigDecimal) row[0]).longValue());
                    } else if (row[0] instanceof Number) {
                        item.setPoid(((Number) row[0]).longValue());
                    }
                }
                // CODE is second column
                if (row.length > 1 && row[1] != null) {
                    item.setCode(row[1].toString());
                }
                // DESCRIPTION is third column
                if (row.length > 2 && row[2] != null) {
                    item.setDescription(row[2].toString());
                }
                items.add(item);
            }
        }

        log.debug("Fetched {} items for LOV: {}", items.size(), lovName);
        return new LovResponse(items);
    }
}

