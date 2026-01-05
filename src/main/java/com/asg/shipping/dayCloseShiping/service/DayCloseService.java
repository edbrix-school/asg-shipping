package com.asg.shipping.dayCloseShiping.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.dayCloseShiping.dto.DayCloseDto;
import com.asg.shipping.dayCloseShiping.dto.DayCloseSummaryProjection;

public interface DayCloseService {

	DayCloseDto getDayClose(Long transactionPoid, Long groupPoid, Long companyPoid);

	DayCloseDto createDayClose(DayCloseDto dto, Long groupPoid, Long companyPoid, Long userPoid);

	DayCloseSummaryProjection getNewDayCloseData(Long groupPoid, Long companyPoid, String transactionDate);

	public List<Map<String, Object>> getDenominations(String currencyCode);

	DayCloseDto updateDayClose(DayCloseDto request, Long transactionPoid, Long groupPoid, Long companyPoid,
			Long userPoid);

	Map<String, Object> searchDayClose(String docId, FilterRequestDto request, Pageable pageable);
}
