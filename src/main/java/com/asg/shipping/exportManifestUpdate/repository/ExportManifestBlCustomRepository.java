package com.asg.shipping.exportManifestUpdate.repository;

import org.springframework.stereotype.Repository;

/**
 * Custom repository for stored procedure calls related to Export Manifest BL
 */
@Repository
public interface ExportManifestBlCustomRepository {

    /**
     * Validates BL number uniqueness
     * Calls PROC_BL_EXPORT_DUPLICATE
     * @param blNumber BL number to validate
     * @param oldBlNumber Old BL number (for updates)
     * @param action Action type: 'INSERTING' or 'UPDATING'
     * @return Status string (should start with 'SUCCESS' if valid)
     */
    String validateBlNumberDuplicate(String blNumber, String oldBlNumber, String action);

    /**
     * Post-save processing
     * Calls PROC_SHIP_BL_PAGE_SAVE_AFTER
     */
    void processAfterSave(Long groupPoid, Long companyPoid, Long transactionPoid, Long detRowId, String updateType, Long userPoid);

    /**
     * Process after quotation LOV browse
     * Calls PROC_LOV_AFTER_BRWS_300_103
     */
    void processQuotationAfterBrowse(Long groupPoid, Long companyPoid, Long userPoid, String docId, Long transactionPoid, String lovName, Long quotationTransactionPoid);

    /**
     * Update BL print status
     * Calls PROC_UPDATE_BL_PRINT_STATUS
     */
    void updateBlPrintStatus(Long transactionPoid);

    /**
     * Export EDI
     * Calls PROC_SHIP_EXPORT_EDI_OUT
     */
    void exportEdi(Long transactionPoid, Long userPoid);

    /**
     * Get BL status
     * Calls PROC_SHIP_EXPORT_BL_STATUS
     * @return Status string
     */
    String getBlStatus(Long groupPoid, Long companyPoid, Long userPoid, Long transactionPoid);

    /**
     * Get BL print report format
     * Calls FUNC_SHIP_GET_BL_PRINT_REPORT
     * @param returnType 'BL_PRINT' or 'BL_RIDER'
     * @return Report format name
     */
    String getBlPrintReport(Long groupPoid, Long companyPoid, String docId, Long transactionPoid, String returnType);
}

