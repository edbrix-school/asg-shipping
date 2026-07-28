package com.asg.shipping.demurrageenquiryblwise.dto;

import com.asg.common.lib.dto.LovGetListDto;
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
	private BigDecimal amount;
	private Long taxPoid;
	private LovGetListDto taxDet;
	private BigDecimal taxPercentage;
	private BigDecimal taxAmount;
	/** amount + taxAmount. */
	private BigDecimal totalAmount;
}
