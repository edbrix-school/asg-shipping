package com.asg.shipping.demurrageenquiryblwise.dto;

import com.asg.common.lib.dto.LovGetListDto;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Row of the Charges tab of the Demurrage Enquiry - BL wise screen (100-144).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DemurrageEnquiryChargeDto {

	/** Serial number of the row (SN column). */
	private Long detRowId;
	private Long blPoid;
	private LovGetListDto blDet;
	private Long chargePoid;
	private LovGetListDto chargeDet;
	/** DET_ROW_ID of the originating BL manifest charge, 0 for derived charges. */
	private Long chargesDetRowId;
	/**
	 * Origin of the row: BLCHARGE (BL manifest charge), SHDEMURRAGE (calculated demurrage) or the
	 * CHARGE_TYPE_APPLICABLE of the port charge tariff (LATECOLLECTIONIMP, REVALIDATEIMP, ...).
	 */
	private String chargeType;
	private String invoiceType;
	@JsonSerialize(using = AmountSerializer.class)
	private BigDecimal amount;
	private Long taxPoid;
	private LovGetListDto taxDet;
	private BigDecimal taxPercentage;
	@JsonSerialize(using = AmountSerializer.class)
	private BigDecimal taxAmount;
	/** amount + taxAmount. */
	@JsonSerialize(using = AmountSerializer.class)
	private BigDecimal totalAmount;
	/**
	 * Remarks column of the Charges tab, derived - nothing stores it:
	 * <ul>
	 *   <li>late collection rows: {@code Applicable from DD-MON-YY onwards}, the day the charge starts
	 *       to apply</li>
	 *   <li>the calculated demurrage row: the slab breakdown of the containers it bills
	 *       ({@code FUNC_RTN_DEM_DETTN_FULL_TEXT}, the text the print shows)</li>
	 *   <li>revalidation and BL manifest charges: empty - the first is not date driven, the second has
	 *       no remarks column at all</li>
	 * </ul>
	 */
	private String remarks;
}
