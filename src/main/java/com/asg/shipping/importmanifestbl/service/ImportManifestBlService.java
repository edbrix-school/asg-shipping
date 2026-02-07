package com.asg.shipping.importmanifestbl.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.importManifestUpdate.dto.*;

import com.asg.shipping.importmanifestbl.dto.ContainersDropDownDto;
import com.asg.shipping.importmanifestbl.dto.DefaultValueDto;
import com.asg.shipping.importmanifestbl.dto.ImportManifestBlDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface ImportManifestBlService {

    ImportManifestBlDto getImportManifest(Long transactionPoId);

    void delete(Long transactionPoId, DeleteReasonDto deleteReasonDto);

    EmailVerificationResponseDto updateEmailVerification(Long transactionPoId, EmailVerificationRequestDto request);

    ResendCanResponseDto resendCan(Long transactionPoId);

    SendEdiEmailsResponseDto sendEdiEmails(Long transactionPoId);

    LoadEmailFaxResponseDto loadEmailFax(Long transactionPoId, LoadEmailFaxRequestDto request);

    BlStatusResponseDto getBlStatus(Long transactionPoId);

    Map<String, Object> list(FilterRequestDto filters, Pageable pageable);

    ImportManifestBlRequestDto createImportManifestBl(ImportManifestBlCreateDto request, Long companyPoid, Long groupPoid);


    ImportManifestBlRequestDto updateImportManifestBl(Long id, @Valid ImportManifestBlUpdateDTO dto, Long companyPoid, Long groupPoid);

    ContainersDropDownDto getContainerTypesByVoyage(Long voyageTransPoid);

    DefaultValueDto getDefaultValues(String docId);
}
