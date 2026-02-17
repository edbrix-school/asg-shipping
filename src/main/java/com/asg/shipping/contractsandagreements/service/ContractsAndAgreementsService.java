package com.asg.shipping.contractsandagreements.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.contractsandagreements.dto.AdminContractsAgreementHdrDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface ContractsAndAgreementsService {
    AdminContractsAgreementHdrDto createContractsAndAgreements(@Valid AdminContractsAgreementHdrDto createDTO);

    AdminContractsAgreementHdrDto getContractsAndAgreementsById(Long transactionPoid);

    void deleteContractsAndAgreements(Long id, DeleteReasonDto deleteReasonDto);

    Map<String, Object> list(FilterRequestDto filters, Pageable pageable);

    AdminContractsAgreementHdrDto updateContractsAndAgreements(Long id, @Valid AdminContractsAgreementHdrDto updateDTO);
}
