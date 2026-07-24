package com.asg.shipping.importmanifestbl.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.importmanifestbl.dto.*;
import com.asg.shipping.importmanifestupdate.dto.*;

import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

public interface ImportManifestService {

    ImportManifestBlDto getImportManifest(Long transactionPoId);

    void delete(Long transactionPoId, DeleteReasonDto deleteReasonDto);

    EmailVerificationResponseDto updateEmailVerification(Long transactionPoId, EmailVerificationRequestDto request);

    ResendCanResponseDto resendCan(Long transactionPoId, String updateDemurrage);

    SendEdiEmailsResponseDto sendEdiEmails(Long transactionPoId);

    LoadEmailFaxResponseDto loadEmailFax(Long addressMasterPoid, String addressType);

    BlStatusResponseDto getBlStatus(Long transactionPoId);

    Map<String, Object> list(FilterRequestDto filters, LocalDate fromDate, LocalDate toDate, Pageable pageable);

    ImportManifestBlResponseDto createImportManifestBl(ImportManifestBlDto request, Long companyPoid, Long groupPoid);

    ImportManifestBlResponseDto updateImportManifestBl(Long id,  ImportManifestBlDto dto);

    ContainersDropDownDto getContainerTypesByVoyage(Long voyageTransPoid);

    DefaultValueDto getDefaultValues(String docId);

    byte[] printUnclearedCargoNotice(Long transactionPoid) throws Exception;

    byte[] printProformaInvoice(Long transactionPoid, LocalDate demChargesTill, Long percentage) throws Exception;

    byte[] printCargoArrivalNotice(Long voyageTransactionPoid,Long transactionPoid) throws Exception;

    byte[] printCargoManifest(Long transactionPoid,boolean isCargoManifestPrint) throws Exception;

    byte[] printCheckPortCharges(Long transactionPoid) throws Exception;

    String saveEmails(Long transactionPoId, SaveEmailsRequestDto request);
    ChargeDefaultsResponseDto getChargeDefaults(ChargeDefaultsRequestDto request);
    java.util.List<ConsigneeEmailDto> getConsigneeEmails(Long transactionPoId);
}
