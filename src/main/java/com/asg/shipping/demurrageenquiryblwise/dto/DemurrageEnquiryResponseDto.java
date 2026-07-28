package com.asg.shipping.demurrageenquiryblwise.dto;

import com.asg.common.lib.dto.LovGetListDto;
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

	private List<DemurrageEnquiryContainerDto> containers;
	private List<DemurrageEnquiryChargeDto> charges;

	/** Total demurrage of all containers, after discount. */
	private BigDecimal totalDemurrageAmount;
	/** Receipt amount - sum of all charge amounts, tax excluded. */
	private BigDecimal receiptAmount;
	/** Sum of the tax of all charges. */
	private BigDecimal totalTaxAmount;
	/** Total (Receipt + VAT) amount. */
	private BigDecimal totalAmountWithVat;
}
