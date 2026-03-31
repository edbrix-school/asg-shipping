package com.asg.shipping.importmanifestupdate.respository;


import com.asg.shipping.importmanifestbl.dto.DefaultValueDto;
import com.asg.shipping.importmanifestupdate.dto.*;

public interface ImportManifestBlProcRepository {
    EmailVerificationResponseDto updateEmailVerification(Long transactionPoId, EmailVerificationRequestDto request);
    ResendCanResponseDto resendCan(Long voyageTransactionPoId, Long transactionPoId);
    SendEdiEmailsResponseDto getEdiEmails(Long transactionPoId);
    BlStatusResponseDto getBlStatus(Long transactionPoId);
    void processBlSaveAfter(Long transactionPoid, Long groupPoid, Long companyPoid, String processType);
    void validateBeforeSave(Long voyageTransactionPoid, Long transactionPoid,Long quotationPoid,String freight,String bookedByPrincipal);
    DefaultValueDto callDefaultGetValue(Long loginGroupPoid, Long loginCompanyPoid, Long loginUserPoid, String docId);
    void saveEmailsToDb(Long transactionPoId, String addressType,
                        String email1, String email2, String scope);
}
