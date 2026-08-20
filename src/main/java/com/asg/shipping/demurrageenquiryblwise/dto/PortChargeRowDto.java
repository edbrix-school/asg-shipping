package com.asg.shipping.demurrageenquiryblwise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Late collection / revalidation charge read from the Container Type Port Charges Tariff
 * (SHIP_PORT_CHARGES_HDR / SHIP_PORT_CHARGES_DTL).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PortChargeRowDto {

	/** LATECOLLECTIONIMP, LATECOLLECTIONBOTH, REVALIDATEIMP or REVALIDATEBOTH. */
	private String chargeTypeApplicable;
	/** PERBL or PERQUENTITY. */
	private String chargeApplicable;
	private Long chargeCodePoid;
	private BigDecimal amount20;
	private BigDecimal amount40;
	private BigDecimal amountOther;
	private Long taxPoid;
	private BigDecimal taxPercentage;
	/** Y when GLOBAL_TAX_APPLICABLE is enabled for the company. */
	private String taxApplicable;
	/**
	 * First day the late collection charge applies: arrival + SHIPLATEDOCOLLECTION - 1, the day the
	 * {@code (SYSDATE - arrival) + 1 >= SHIPLATEDOCOLLECTION} filter of the query starts to hold.
	 * Null on the revalidation rows, which are not date driven.
	 */
	private LocalDate applicableFrom;
}
