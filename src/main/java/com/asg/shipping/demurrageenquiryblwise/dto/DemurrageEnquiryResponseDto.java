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
 * Full enquiry result of the Demurrage Enquiry - BL wise screen (100-144): BL details, the
 * Containers tab, the Charges tab and the charge totals.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DemurrageEnquiryResponseDto {

	private Long blPoid;
	private LovGetListDto blDet;
	/** DOISSUED / DONOTISSUED - whether the Delivery Order has already been printed for the BL. */
	private String doStatus;
	/** To Date used for the calculation. */
	private LocalDate toDate;
	/** Discount percentage used for the calculation. */
	private BigDecimal discountPercentage;
	/** Free days applied instead of the tariff free days, null when the tariff free days were used. */
	private Integer freeDays;

	private List<DemurrageEnquiryContainerDto> containers;
	private List<DemurrageEnquiryChargeDto> charges;

	/** Total demurrage of all containers, after discount. */
	@JsonSerialize(using = AmountSerializer.class)
	private BigDecimal totalDemurrageAmount;
	/** Receipt amount - sum of all charge amounts, tax excluded. */
	@JsonSerialize(using = AmountSerializer.class)
	private BigDecimal receiptAmount;
	/** Sum of the tax of all charges. */
	@JsonSerialize(using = AmountSerializer.class)
	private BigDecimal totalTaxAmount;
	/** Total (Receipt + VAT) amount. */
	@JsonSerialize(using = AmountSerializer.class)
	private BigDecimal totalAmountWithVat;
}
