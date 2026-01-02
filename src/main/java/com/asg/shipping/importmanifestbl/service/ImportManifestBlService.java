package com.asg.shipping.importmanifestbl.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.importManifestUpdate.dto.*;

import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface ImportManifestBlService {

    ImportManifestBlRequestDto getImportManifest(Long transactionPoId);

    void delete(Long transactionPoId);

    EmailVerificationResponseDto updateEmailVerification(Long transactionPoId, EmailVerificationRequestDto request);

    ResendCanResponseDto resendCan(Long transactionPoId);

    SendEdiEmailsResponseDto sendEdiEmails(Long transactionPoId);

    LoadEmailFaxResponseDto loadEmailFax(Long transactionPoId, LoadEmailFaxRequestDto request);

    BlStatusResponseDto getBlStatus(Long transactionPoId);

    ImportManifestBlRequestDto createImportManifestBl(ImportManifestBlCreateDto request, Long companyPoid, Long groupPoid);

}
