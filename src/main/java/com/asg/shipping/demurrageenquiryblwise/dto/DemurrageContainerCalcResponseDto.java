package com.asg.shipping.demurrageenquiryblwise.dto;

import com.asg.common.lib.dto.LovGetListDto;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Result of the row wise calculation: the requested container rows with their demurrage period,
 * days, amounts and slab breakdown filled, ready to be dropped back into the Containers grid.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DemurrageContainerCalcResponseDto {

	private Long blPoid;
	private LovGetListDto blDet;
	/** To Date used for the rows that carry none of their own. */
	private LocalDate toDate;
	/** Discount percentage used for the calculation. */
	private BigDecimal discountPercentage;
	/** Free days applied to the rows that carry none of their own, null when the tariff free days were used. */
	private Integer freeDays;

	/**
	 * The requested rows, in the order they were requested, with the demurrage columns filled. The
	 * To Date every row was calculated up to is its own {@code dmToDate}.
	 */
	private List<DemurrageEnquiryContainerDto> containers;

	/**
	 * Total demurrage of the requested rows after discount. It is the total of these rows only, not
	 * of the BL - the charges of the BL keep coming from "Apply Date".
	 */
	@JsonSerialize(using = AmountSerializer.class)
	private BigDecimal totalDemurrageAmount;
}
