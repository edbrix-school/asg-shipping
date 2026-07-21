package com.asg.shipping.exportManifestBl.repository;

import java.time.LocalDate;
import java.util.List;

public interface ExportManifestBlProcRepository {

    Object[] getTaxRate(Long chargePoid, Long companyPoid, LocalDate transactionDate);

    void processAfterSave(Long groupPoid, Long companyPoid, Long transactionPoid, Long detRowId,
                          String updateType, Long userPoid);

   
    void processExportLocalCharge(Long groupPoid, Long companyPoid, Long transactionPoid);

    void loadCustomerAutoCharges(Long groupPoid, Long companyPoid, Long transactionPoid, Long detRowId,
                                 String updateType, Long userPoid);

    List<String> loadDamageClauseLines();
}
