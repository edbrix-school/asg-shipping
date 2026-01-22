package com.asg.shipping.importManifestUpdate.service;

import com.asg.shipping.importManifestUpdate.dto.*;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface ImportManifestBlService {

    ImportManifestBlRequestDto createImportManifestBl(ImportManifestBlCreateDto dto);

    ImportManifestBlRequestDto updateImportManifestBl(Long id, ImportManifestBlUpdateDTO dto, Long companyPoid, Long groupPoid);

    Map<String, Object> listOfImportManifest(String docId, FilterRequestDto request, Pageable pageable);

    ImportManifestBlRequestDto getImportManifestBl(Long id);

    void deleteImportManifestBl(Long id);

    EmailVerificationResponseDto updateEmailVerification(Long transactionPoId, EmailVerificationRequestDto request);

    ResendCanResponseDto resendCan(Long transactionPoId);

    SendEdiEmailsResponseDto sendEdiEmails(Long transactionPoId);

    LoadEmailFaxResponseDto loadEmailFax(Long transactionPoId, LoadEmailFaxRequestDto request);

    BlStatusResponseDto getBlStatus(Long transactionPoId);

}
