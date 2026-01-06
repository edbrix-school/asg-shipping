package com.asg.shipping.vesselvoyagecreation.repository;

import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class StoredProcedureRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public String procGlobUserLineListing(Long loginUserPoid) {
        StoredProcedureQuery q = entityManager.createStoredProcedureQuery("PROC_GLOB_USER_LINE_LISTING");
        q.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(2, String.class, ParameterMode.OUT);
        q.setParameter(1, loginUserPoid);
        q.execute();
        return (String) q.getOutputParameterValue(2);
    }

    public String procAttachmentsEdiProcNew(Long groupPoid, Long companyPoid, String docId, Long docKeyPoid, Long jobPoid, String loginUser) {
        StoredProcedureQuery q = entityManager.createStoredProcedureQuery("PROC_ATTACHMENTS_EDI_PROC_NEW");
        q.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(3, String.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(4, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(5, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(6, String.class, ParameterMode.OUT);
        q.registerStoredProcedureParameter(7, String.class, ParameterMode.IN);
        q.setParameter(1, groupPoid);
        q.setParameter(2, companyPoid);
        q.setParameter(3, docId);
        q.setParameter(4, docKeyPoid);
        q.setParameter(5, jobPoid);
        q.setParameter(7, loginUser);
        q.execute();
        return (String) q.getOutputParameterValue(6);
    }

    public void prodResendCan(Long voyageTransactionPoid, Long blTransactionPoid) {
        StoredProcedureQuery q = entityManager.createStoredProcedureQuery("PROD_RESEND_CAN");
        q.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(3, String.class, ParameterMode.IN);
        q.setParameter(1, voyageTransactionPoid);
        q.setParameter(2, blTransactionPoid);
        q.setParameter(3, "C");
        q.execute();
    }

    public String procMateRcptEmptyManifest(Long voyagePoid, String userCode) {
        StoredProcedureQuery q = entityManager.createStoredProcedureQuery("PROC_MATE_RCPT_EMPTY_MANIFEST");
        q.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(3, String.class, ParameterMode.OUT);
        q.setParameter(1, voyagePoid);
        q.setParameter(2, userCode);
        q.execute();
        return (String) q.getOutputParameterValue(3);
    }

    public String procLoadTdrOwnLine(Long voyagePoid, Long loginUserPoid) {
        StoredProcedureQuery q = entityManager.createStoredProcedureQuery("PROC_LOAD_TDR_OWN_LINE");
        q.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(3, String.class, ParameterMode.OUT);
        q.setParameter(1, voyagePoid);
        q.setParameter(2, loginUserPoid);
        q.execute();
        return (String) q.getOutputParameterValue(3);
    }

    public void procShipExportEdiCosco(Long voyagePoid, Long blPoid, Long loginUserPoid) {
        StoredProcedureQuery q = entityManager.createStoredProcedureQuery("PROC_SHIP_EXPORT_EDI_COSCO");
        q.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(3, Long.class, ParameterMode.IN);
        q.setParameter(1, voyagePoid);
        q.setParameter(2, blPoid);
        q.setParameter(3, loginUserPoid);
        q.execute();
    }

    public String procGeneralImportCargo(Long groupPoid, Long companyPoid, String loginUser, Long voyagePoid) {
        StoredProcedureQuery q = entityManager.createStoredProcedureQuery("PROC_GENERAL_IMPORT_CARGO");
        q.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(3, String.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(4, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(5, String.class, ParameterMode.OUT);
        q.setParameter(1, groupPoid);
        q.setParameter(2, companyPoid);
        q.setParameter(3, loginUser);
        q.setParameter(4, voyagePoid);
        q.execute();
        return (String) q.getOutputParameterValue(5);
    }

    public void procShipVoyageCurrencyUpd(Long groupPoid, Long companyPoid, Long voyagePoid, String userCode, String recordsUpdate) {
        StoredProcedureQuery q = entityManager.createStoredProcedureQuery("proc_ship_voyage_currency_upd");
        q.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(3, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(5, String.class, ParameterMode.IN);
        q.setParameter(1, groupPoid);
        q.setParameter(2, companyPoid);
        q.setParameter(3, voyagePoid);
        q.setParameter(4, userCode);
        q.setParameter(5, recordsUpdate);
        q.execute();
    }

    public String procShipVoyageHnjnTranImpV2(Long voyagePoid) {
        StoredProcedureQuery q = entityManager.createStoredProcedureQuery("PROC_SHIP_VOYAGE_HNJN_TRAN_IMP_V2");
        q.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(2, String.class, ParameterMode.OUT);
        q.setParameter(1, voyagePoid);
        q.execute();
        return (String) q.getOutputParameterValue(2);
    }

    public String procLoadEdiXlTemplate(Long voyagePoid) {
        StoredProcedureQuery q = entityManager.createStoredProcedureQuery("PROC_LOAD_EDI_XL_TEMPLATE");
        q.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(2, String.class, ParameterMode.OUT);
        q.setParameter(1, voyagePoid);
        q.execute();
        return (String) q.getOutputParameterValue(2);
    }
}










