package com.asg.shipping.demurrageenquiryblwise.dto;

import com.asg.common.lib.dto.LovGetListDto;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Row of the Containers tab of the Demurrage Enquiry - BL wise screen (100-144).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DemurrageEnquiryContainerDto {

	/** Serial number of the row (SN column). */
	private Long detRowId;
	private Long blPoid;
	private LovGetListDto blDet;
	/** SOC flag - EQUIPMENT_SHIPPER_OWN. */
	private String containerSocYn;
	private String containerNo;
	private String equipmentIsoType;
	private Long freeDays;
	/** Demurrage calculation start date. */
	private LocalDate dmFrmDate;
	/** Demurrage calculation end date - the applied To Date, or Empty In when the container is back. */
	private LocalDate dmToDate;
	/** Number of days liable for demurrage. */
	private Long dmDays;
	/** Demurrage amount after the applied discount. */
	@JsonSerialize(using = AmountSerializer.class)
	private BigDecimal dmChargeAmt;
	/** Demurrage amount before the applied discount. */
	@JsonSerialize(using = AmountSerializer.class)
	private BigDecimal dmChargeAmtBeforeDiscount;
	/** Date on which the container was returned empty (MTIN move). */
	private LocalDate emptyIn;
	/**
	 * Slab breakdown of the demurrage amount, as {@code FUNC_RTN_DEM_DETTN_FULL_TEXT} writes it - the
	 * same text the Remarks column of the View Demurrage Calculation print shows. Empty while the
	 * container is inside its free days, there being no slab to explain.
	 */
	private String remarks;
}
