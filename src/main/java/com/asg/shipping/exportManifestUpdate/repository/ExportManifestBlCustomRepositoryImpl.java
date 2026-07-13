package com.asg.shipping.exportManifestUpdate.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import java.sql.ResultSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * Implementation of custom repository for stored procedure calls
 */
@Repository
public class ExportManifestBlCustomRepositoryImpl implements ExportManifestBlCustomRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final String defaultSchema;

    public ExportManifestBlCustomRepositoryImpl(
            @Value("${spring.jpa.properties.hibernate.default_schema}") String defaultSchema) {
        this.defaultSchema = defaultSchema;
    }

    @Override
    public String validateBlNumberDuplicate(String blNumber, String oldBlNumber, String action) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_BL_EXPORT_DUPLICATE");
        
        query.registerStoredProcedureParameter("P_BL_NUMBER", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_OLD_BL_NUMBER", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_ACTION", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT);
        
        query.setParameter("P_BL_NUMBER", blNumber);
        query.setParameter("P_OLD_BL_NUMBER", oldBlNumber != null ? oldBlNumber : "NEWRECORD");
        query.setParameter("P_ACTION", action);
        
        query.execute();
        
        return (String) query.getOutputParameterValue("P_STATUS");
    }

    @Override
    public void processAfterSave(Long groupPoid, Long companyPoid, Long transactionPoid, Long detRowId, String updateType, Long userPoid) {
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
        query.setParameter("P_UPDATE_TYPE", updateType != null ? updateType : "AUTOSUMWEIGHTPEXPORT");
        query.setParameter("P_LOGIN_USER", userPoid);
        
        query.execute();
    }

    @Override
    public void processQuotationAfterBrowse(Long groupPoid, Long companyPoid, Long userPoid, String docId, Long transactionPoid, String lovName, Long quotationTransactionPoid) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_LOV_AFTER_BRWS_300_103");
        
        query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOV_NAME", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOV_VALUE", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("OUTDATA", ResultSet.class, ParameterMode.REF_CURSOR);
        
        query.setParameter("P_LOGIN_GROUP_POID", groupPoid);
        query.setParameter("P_LOGIN_COMPANY_POID", companyPoid);
        query.setParameter("P_LOGIN_USER_POID", userPoid);
        query.setParameter("P_DOC_ID", docId);
        query.setParameter("P_DOC_KEY_POID", transactionPoid);
        query.setParameter("P_LOV_NAME", lovName);
        query.setParameter("P_LOV_VALUE", quotationTransactionPoid != null ? quotationTransactionPoid.toString() : null);
        
        query.execute();
    }

    @Override
    public void updateBlPrintStatus(Long transactionPoid) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_UPDATE_BL_PRINT_STATUS");
        
        query.registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN);
        query.setParameter("P_TRANSACTION_POID", transactionPoid);
        
        query.execute();
    }

    @Override
    public void exportEdi(Long transactionPoid, Long userPoid) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_SHIP_EXPORT_EDI_OUT");
        
        query.registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_USER_POID", Long.class, ParameterMode.IN);
        
        query.setParameter("P_TRANSACTION_POID", transactionPoid);
        query.setParameter("P_USER_POID", userPoid);
        
        query.execute();
    }

    @Override
    public String getBlStatus(Long groupPoid, Long companyPoid, Long userPoid, Long transactionPoid) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_SHIP_EXPORT_BL_STATUS");
        
        query.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_USER_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT);
        
        query.setParameter("P_GROUP_POID", groupPoid);
        query.setParameter("P_COMPANY_POID", companyPoid);
        query.setParameter("P_USER_POID", userPoid);
        query.setParameter("P_TRANSACTION_POID", transactionPoid);
        
        query.execute();
        
        return (String) query.getOutputParameterValue("P_STATUS");
    }

    @Override
    public String getBlPrintReport(Long groupPoid, Long companyPoid, String docId, Long transactionPoid, String returnType) {
    	String sql = """
				SELECT %s.FUNC_SHIP_GET_BL_PRINT_REPORT(
				    :groupPoid,
				    :companyPoid,
				    :docId,
				    :transactionPoid,
				    :returnType
				) FROM DUAL
				""".formatted(defaultSchema);

		return (String) entityManager.createNativeQuery(sql).setParameter("groupPoid", groupPoid)
				.setParameter("companyPoid", companyPoid).setParameter("docId", docId)
				.setParameter("transactionPoid", transactionPoid).setParameter("returnType", returnType)
				.getSingleResult();
    }
}

