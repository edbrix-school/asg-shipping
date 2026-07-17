package com.asg.shipping.daycloseshiping.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.daycloseshiping.dto.DayCloseDto;
import com.asg.shipping.daycloseshiping.dto.DayCloseSummaryProjection;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DayCloseService {

    DayCloseDto getDayClose(Long transactionPoid, Long groupPoid, Long companyPoid);

    DayCloseDto createDayClose(DayCloseDto dto, Long groupPoid, Long companyPoid, Long userPoid);

    DayCloseSummaryProjection getNewDayCloseData(Long groupPoid, Long companyPoid, String transactionDate);

    public List<Map<String, Object>> getDenominations(String currencyCode);

    DayCloseDto updateDayClose(DayCloseDto request, Long transactionPoid, Long groupPoid, Long companyPoid,
                               Long userPoid);

    Map<String, Object> searchDayClose(String docId, FilterRequestDto request, Pageable pageable, LocalDate startDate, LocalDate endDate);

    byte[] print(Long transactionPoid) throws Exception;

    byte[] printDetails(Long transactionPoid) throws Exception;

    byte[] printSplitReceipt(Long transactionPoid) throws Exception;

    byte[] printSummary(Long transactionPoid) throws Exception;

    void deleteDayClose(Long id, DeleteReasonDto deleteReasonDto);
}
