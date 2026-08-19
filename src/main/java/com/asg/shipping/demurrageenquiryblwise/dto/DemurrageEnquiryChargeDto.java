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
	 * Remarks column of the Charges tab. Filled on the calculated demurrage row with the slab
	 * breakdown of the containers it bills ({@code FUNC_RTN_DEM_DETTN_FULL_TEXT}, the text the print
	 * shows). Empty on every other row: the legacy grid binds those to
	 * {@code AR_SH_RECEIPT_CHARGES_DTL.REMARKS}, which only exists once a receipt has been saved, and
	 * the BL manifest charges have no remarks column at all.
	 */
	private String remarks;
}
