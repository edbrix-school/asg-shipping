package com.asg.shipping.lineprofile.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.lineprofile.dto.LineProfileAgreementDetailsResponse;
import com.asg.shipping.lineprofile.dto.LineProfileDrilldownResponse;
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

    void delete(Long lineProfilePoid, DeleteReasonDto deleteReasonDto);

    LineProfileLineDetailsResponse fetchLineDetails(Long linePoid, Long groupPoid, Long companyPoid, Long userPoid);

    LineProfileAgreementDetailsResponse fetchAgreementDetails(Long transactionPoid, Long groupPoid, Long companyPoid, Long userPoid);

    /**
     * Calls PROC_LINE_PROFILE_DRILLDOWN_FORM and returns both:
     * <ul>
     *   <li>P_STATUS  – "Success", a warning, or an "ERROR :" message</li>
     *   <li>OUTDATA   – list of rows with TRANSACTION_POID, COMPANY_POID, RECORD_FETCH_TYPE</li>
     * </ul>
     *
     * @param groupPoid    login group poid
     * @param companyPoid  login company poid
     * @param userPoid     login user poid
     * @param docId        document id (P_DOC_ID)
     * @param linePoid     line poid (LINE_POID)
     * @param returnRecord one of: TARIFF, LOCAL, COMMISSION
     * @return {@link LineProfileDrilldownResponse} containing status and cursor data
     */
    LineProfileDrilldownResponse fetchDrilldownForm(Long groupPoid, Long companyPoid, Long userPoid,
                                                    String docId, Long linePoid, String returnRecord);
}

