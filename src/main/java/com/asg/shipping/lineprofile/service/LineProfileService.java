package com.asg.shipping.lineprofile.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.lineprofile.dto.LineProfileAgreementDetailsResponse;
import com.asg.shipping.lineprofile.dto.LineProfileLineDetailsResponse;
import com.asg.shipping.lineprofile.dto.LineProfileRequest;
import com.asg.shipping.lineprofile.dto.LineProfileResponse;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface LineProfileService {
    Map<String, Object> listLineProfiles(String docId, FilterRequestDto filters, Pageable pageable);

    LineProfileResponse getById(Long lineProfilePoid, Long groupPoid);

    LineProfileResponse create(LineProfileRequest request, Long groupPoid, String userId, String docId);

    LineProfileResponse update(Long lineProfilePoid, LineProfileRequest request, Long groupPoid, String userId, String docId);

    void delete(Long lineProfilePoid, Long groupPoid, String userId);

    LineProfileLineDetailsResponse fetchLineDetails(Long linePoid, Long groupPoid, Long companyPoid, Long userPoid);

    LineProfileAgreementDetailsResponse fetchAgreementDetails(Long transactionPoid, Long groupPoid, Long companyPoid, Long userPoid);
}

