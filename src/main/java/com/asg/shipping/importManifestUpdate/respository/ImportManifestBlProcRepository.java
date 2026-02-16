package com.asg.shipping.importManifestUpdate.respository;

import com.asg.shipping.importManifestUpdate.dto.EmailVerificationRequestDto;
import com.asg.shipping.importManifestUpdate.dto.EmailVerificationResponseDto;
import com.asg.shipping.importManifestUpdate.dto.ResendCanResponseDto;
import com.asg.shipping.importManifestUpdate.dto.SendEdiEmailsResponseDto;
import com.asg.shipping.importManifestUpdate.dto.BlStatusResponseDto;
import com.asg.shipping.importManifestUpdate.dto.ImportManifestBlCreateDto;
import com.asg.shipping.importmanifestbl.dto.DefaultValueDto;

public interface ImportManifestBlProcRepository {
    EmailVerificationResponseDto updateEmailVerification(Long transactionPoId, EmailVerificationRequestDto request);
    ResendCanResponseDto resendCan(Long voyageTransactionPoId, Long transactionPoId);
    SendEdiEmailsResponseDto sendEdiEmails(Long transactionPoId);
    BlStatusResponseDto getBlStatus(Long transactionPoId);
    void processBlSaveAfter(Long transactionPoid, Long groupPoid, Long companyPoid, String processType);
    void validateBeforeSave(ImportManifestBlCreateDto dto, Long transactionPoid);
    DefaultValueDto callDefaultGetValue(Long loginGroupPoid, Long loginCompanyPoid, Long loginUserPoid, String docId);
}
