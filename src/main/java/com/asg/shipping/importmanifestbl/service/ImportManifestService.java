package com.asg.shipping.importmanifestbl.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.importmanifestupdate.dto.*;

import com.asg.shipping.importmanifestbl.dto.ContainersDropDownDto;
import com.asg.shipping.importmanifestbl.dto.DefaultValueDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

public interface ImportManifestService {

    ImportManifestBlRequestDto getImportManifest(Long transactionPoId);

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

    byte[] printUnclearedCargoNotice(Long transactionPoid) throws Exception;

    byte[] printProformaInvoice(Long transactionPoid, LocalDate demChargesTill, Long percentage) throws Exception;

    byte[] printCargoArrivalNotice(Long voyageTransactionPoid,Long transactionPoid) throws Exception;

    byte[] printCargoManifest(Long transactionPoid,boolean isCargoManifestPrint) throws Exception;

    byte[] printCheckPortCharges(Long transactionPoid) throws Exception;

    String saveEmails(Long transactionPoId, SaveEmailsRequestDto request);
}
