package com.asg.shipping.vesselvoyagecreation.repository;

import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

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

    public String procAttachmentsEdiProcNew(Long groupPoid, Long companyPoid, String docId, Long docKeyPoid, Long jobPoid, Long userPoid) {
        StoredProcedureQuery q = entityManager.createStoredProcedureQuery("PROC_ATTACHMENTS_EDI_PROC_NEW");
        q.setHint("jakarta.persistence.query.timeout", 600000);
        q.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(3, String.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(4, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(5, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(6, String.class, ParameterMode.OUT);
        q.registerStoredProcedureParameter(7, String.class, ParameterMode.IN); // P_LOGIN_USER is VARCHAR2 in DB
        q.setParameter(1, groupPoid);
        q.setParameter(2, companyPoid);
        q.setParameter(3, docId);
        q.setParameter(4, docKeyPoid);
        q.setParameter(5, jobPoid);
        q.setParameter(7, userPoid != null ? userPoid.toString() : "0");
        q.execute();
        return (String) q.getOutputParameterValue(6);
    }

    public void prodResendCan(Long voyageTransactionPoid, Long blTransactionPoid) {
        StoredProcedureQuery q = entityManager.createStoredProcedureQuery("PROD_RESEND_CAN");
        q.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
        q.setParameter(1, voyageTransactionPoid);
        q.setParameter(2, blTransactionPoid);
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

    /**
     * Legacy: PROC_SHIP_BL_PAGE_SAVE_AFTER — called in DocumentAfterSave when MSC voyage ref is present.
     * Triggers MSC data load after a voyage save.
     */
    public void procShipBlPageSaveAfter(Long groupPoid, Long companyPoid, Long voyagePoid, String mscRef, Long userPoid) {
        StoredProcedureQuery q = entityManager.createStoredProcedureQuery("PROC_SHIP_BL_PAGE_SAVE_AFTER");
        q.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(3, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(4, String.class, ParameterMode.IN); // null
        q.registerStoredProcedureParameter(5, String.class, ParameterMode.IN); // "MSCDATALOAD~" + mscRef
        q.registerStoredProcedureParameter(6, Long.class, ParameterMode.IN);
        q.setParameter(1, groupPoid);
        q.setParameter(2, companyPoid);
        q.setParameter(3, voyagePoid);
        q.setParameter(4, null);
        q.setParameter(5, "MSCDATALOAD~" + mscRef);
        q.setParameter(6, userPoid);
        q.execute();
    }

    /**
     * Legacy: PROC_SHIP_DATA_TRN_EDI — called in updateFetchVoyageData to fetch/sync MSC voyage data.
     * Returns a result string; caller should check for "ERROR".
     */
    public String procShipDataTrnEdi(Long groupPoid, Long companyPoid, Long voyagePoid, String mscRef, Long userPoid) {
        StoredProcedureQuery q = entityManager.createStoredProcedureQuery("PROC_SHIP_DATA_TRN_EDI");
        q.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(3, Long.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(4, String.class, ParameterMode.IN); // null
        q.registerStoredProcedureParameter(5, String.class, ParameterMode.IN); // "MSCDATALOAD~" + mscRef
        q.registerStoredProcedureParameter(6, String.class, ParameterMode.OUT);
        q.registerStoredProcedureParameter(7, Long.class, ParameterMode.IN);
        q.setParameter(1, groupPoid);
        q.setParameter(2, companyPoid);
        q.setParameter(3, voyagePoid);
        q.setParameter(4, null);
        q.setParameter(5, "MSCDATALOAD~" + mscRef);
        q.setParameter(7, userPoid);
        q.execute();
        return (String) q.getOutputParameterValue(6);
    }

    /**
     * Inserts a GLOBAL_ATTACHMENTS record with ACTIVE='N' so LINE_EDI_READ_TRANSFER
     * EDI_FILE_COPY cursor picks it up for processing.
     * FILE_NAME_MAPPED is set to the original filename so the full path resolves as:
     * LINUX_AttachmentsPath + FILE_NAME_MAPPED
     */
    public void insertEdiAttachment(Long groupPoid, Long companyPoid, Long voyagePoid,
                                    String fileName, String createdBy) {
        entityManager.createNativeQuery(
                "INSERT INTO GLOBAL_ATTACHMENTS " +
                "(GROUP_POID, COMPANY_POID, DOC_ID, DOC_KEY_POID, FILE_NAME, FILE_NAME_MAPPED, " +
                " CHECKLIST_NAME, EDI_JOB_POID, ACTIVE, DELETED, CREATED_BY, CREATED_DATE) " +
                "VALUES (:groupPoid, :companyPoid, '100-101', :voyagePoid, :fileName, :fileName, " +
                " 'EDI', :voyagePoid, 'N', 'N', :createdBy, SYSDATE)"
        )
        .setParameter("groupPoid", groupPoid)
        .setParameter("companyPoid", companyPoid)
        .setParameter("voyagePoid", voyagePoid)
        .setParameter("fileName", fileName)
        .setParameter("createdBy", createdBy)
        .executeUpdate();
        log.info("EDI attachment record inserted | voyagePoid={} fileName={}", voyagePoid, fileName);
    }

    /**
     * Fetches the EDI upload directory path from GLOBAL_PARAMETERS table.
     * The DB procedure LINE_EDI_READ_TRANSFER reads files from this directory.
     * On Linux servers: LINUX_LINE_EDI_FOLDER = /cloudfs/EDI_ALL/LINE_EDI
     * On Windows servers: LINE_EDI_FOLDER = E:\LINE_EDI
     * Tries Linux path first, falls back to Windows path.
     */
    public String getEdiUploadDirectory(Long groupPoid) {
        String groupPoidStr = groupPoid.toString();
        // Try Linux path first (production servers are Linux)
        @SuppressWarnings("unchecked")
        List<String> linuxResult = entityManager.createNativeQuery(
                "SELECT PARAMETER_VALUE FROM GLOBAL_PARAMETERS " +
                "WHERE PARAMETER_NAME = 'LINUX_LINE_EDI_FOLDER' " +
                "AND PARAMETER_KEYID_TYPE = 'Company' " +
                "AND PARAMETER_KEYID = :groupPoid " +
                "AND NVL(DELETED,'N') = 'N' " +
                "AND ROWNUM = 1"
        ).setParameter("groupPoid", groupPoidStr).getResultList();

        if (linuxResult != null && !linuxResult.isEmpty() && linuxResult.get(0) != null) {
            return linuxResult.get(0).trim();
        }

        // Fall back to Windows path
        @SuppressWarnings("unchecked")
        List<String> winResult = entityManager.createNativeQuery(
                "SELECT PARAMETER_VALUE FROM GLOBAL_PARAMETERS " +
                "WHERE PARAMETER_NAME = 'LINE_EDI_FOLDER' " +
                "AND PARAMETER_KEYID_TYPE = 'Company' " +
                "AND PARAMETER_KEYID = :groupPoid " +
                "AND NVL(DELETED,'N') = 'N' " +
                "AND ROWNUM = 1"
        ).setParameter("groupPoid", groupPoidStr).getResultList();

        return (winResult == null || winResult.isEmpty() || winResult.get(0) == null)
                ? null : winResult.get(0).trim();
    }

    /**
     * Legacy: PDA_ENTRY_HDR TDR reference lookup — MIN(DOC_REF) for a voyage, REF_TYPE='TDR'.
     * Returns "NO_TDR" when no TDR exists.
     */
    /**
     * Legacy: EdiMovesLoadDischarge — PROC_SHIP_CSCL_EDI_OUT_PP(ediDate, userPoid)
     * Triggers EDI moves load/discharge export for CSCL line.
     */
    public void procShipCsclEdiOutPp(String ediDateValue, Long loginUserPoid) {
        StoredProcedureQuery q = entityManager.createStoredProcedureQuery("PROC_SHIP_CSCL_EDI_OUT_PP");
        q.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
        q.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
        q.setParameter(1, ediDateValue);
        q.setParameter(2, loginUserPoid);
        q.execute();
    }

    /**
     * Dynamically calls a named procedure with (companyPoid, voyagePoid, userPoid) signature.
     * Used for Excel file generation procedures looked up from GLOBAL_DOC_MASTER.
     */
    public void callExcelGenerationProc(String procName, Long companyPoid, Long voyagePoid, Long userPoid) {
        entityManager.createNativeQuery(
                "BEGIN " + procName + "(:companyPoid, :voyagePoid, :userPoid); END;"
        )
        .setParameter("companyPoid", companyPoid)
        .setParameter("voyagePoid", voyagePoid)
        .setParameter("userPoid", userPoid)
        .executeUpdate();
    }

    public String findTdrDocRef(Long voyagePoid) {
        @SuppressWarnings("unchecked")
        List<String> result = entityManager.createNativeQuery(
                "SELECT NVL(MIN(DOC_REF),'NO_TDR') FROM PDA_ENTRY_HDR " +
                "WHERE VOYAGE_POID = :voyagePoid AND NVL(DELETED,'N') = 'N' AND REF_TYPE = 'TDR'"
        ).setParameter("voyagePoid", voyagePoid).getResultList();
        return (result == null || result.isEmpty()) ? "NO_TDR" : result.get(0);
    }

    /**
     * Looks up the stored procedure name configured for a given doc ID in GLOBAL_DOC_MASTER.
     * Legacy DownloadCustomExcelFile used this to find which procedure generates the Excel file.
     */
    public String findExcelProcedureForDocId(String docId) {
        @SuppressWarnings("unchecked")
        List<String> result = entityManager.createNativeQuery(
                "SELECT REPORT_PROC_NAME FROM GLOBAL_DOC_MASTER " +
                "WHERE DOC_ID = :docId AND NVL(DELETED,'N') = 'N' AND ROWNUM = 1"
        ).setParameter("docId", docId).getResultList();
        return (result == null || result.isEmpty()) ? null : result.get(0);
    }
}










