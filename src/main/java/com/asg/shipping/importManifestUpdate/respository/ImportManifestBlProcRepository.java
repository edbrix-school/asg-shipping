package com.asg.shipping.importManifestUpdate.respository;

import com.asg.shipping.importManifestUpdate.dto.EmailVerificationRequestDto;
import com.asg.shipping.importManifestUpdate.dto.EmailVerificationResponseDto;
import com.asg.shipping.importManifestUpdate.dto.ResendCanResponseDto;
import com.asg.shipping.importManifestUpdate.dto.SendEdiEmailsResponseDto;
import com.asg.shipping.importManifestUpdate.dto.BlStatusResponseDto;

public interface ImportManifestBlProcRepository {
    EmailVerificationResponseDto updateEmailVerification(Long transactionPoId, EmailVerificationRequestDto request);
    ResendCanResponseDto resendCan(Long voyageTransactionPoId, Long transactionPoId);
    SendEdiEmailsResponseDto sendEdiEmails(Long transactionPoId);
    BlStatusResponseDto getBlStatus(Long transactionPoId);
}
