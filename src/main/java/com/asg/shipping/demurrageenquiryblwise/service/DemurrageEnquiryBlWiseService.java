package com.asg.shipping.demurrageenquiryblwise.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.demurrageenquiryblwise.dto.DemurrageContainerCalcRequestDto;
import com.asg.shipping.demurrageenquiryblwise.dto.DemurrageContainerCalcResponseDto;
import com.asg.shipping.demurrageenquiryblwise.dto.DemurrageEnquiryRequestDto;
import com.asg.shipping.demurrageenquiryblwise.dto.DemurrageEnquiryResponseDto;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Demurrage Enquiry - BL wise (100-144). Read only document: it summarises the demurrage of a BL
 * container wise together with the charges that would be collected for it.
 */
public interface DemurrageEnquiryBlWiseService {

	/**
	 * Loads the BL details and its containers, as done when a BL is picked on the screen. The
	 * demurrage columns (To Date, Days, Amount) are left empty until the enquiry is applied.
	 */
	DemurrageEnquiryResponseDto getBlDetails(Long blPoid);

	/**
	 * "Apply Date" action: recalculates the demurrage of every container up to the given To Date,
	 * applies the discount and reloads the charges of the BL with the totals.
	 */
	DemurrageEnquiryResponseDto applyDate(DemurrageEnquiryRequestDto request);

	/**
	 * Row wise calculation: recalculates the demurrage of the given container rows of the BL only -
	 * one row, or as many as the caller sends - with the discount and the free days of the request.
	 * The charges of the BL are not rebuilt, that stays with {@link #applyDate}.
	 */
	DemurrageContainerCalcResponseDto calculateSelectedContainers(DemurrageContainerCalcRequestDto request);

	/**
	 * "View Demurrage Calculation" link: the demurrage tariff calculation PDF of the BL.
	 */
	byte[] printDemurrageCalculation(Long blPoid, LocalDate toDate, BigDecimal discountPercentage, Integer freeDays)
			throws Exception;

	Map<String, Object> list(FilterRequestDto filters, Pageable pageable, LocalDate startDate, LocalDate endDate);
}
