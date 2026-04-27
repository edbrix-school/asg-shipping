package com.asg.shipping.importmanifestupdate.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.shipping.importmanifestupdate.dto.*;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface ImportManifestBlService {

    ImportManifestBlRequestDto updateImportManifestBl(Long id, ImportManifestBlUpdateDTO dto, Long companyPoid, Long groupPoid);

    Map<String, Object> listOfImportManifest(String docId, FilterRequestDto request, Pageable pageable);

    ImportManifestBlRequestDto getImportManifestBl(Long id);

    void deleteImportManifestBl(Long id, DeleteReasonDto deleteReasonDto);

    EmailVerificationResponseDto updateEmailVerification(Long transactionPoId, EmailVerificationRequestDto request);

    ResendCanResponseDto resendCan(Long transactionPoId, String updateDemurrage);

    SendEdiEmailsResponseDto sendEdiEmails(Long transactionPoId);

    LoadEmailFaxResponseDto loadEmailFax(Long addressMasterPoid, String addressType);

    BlStatusResponseDto getBlStatus(Long transactionPoId);

}
