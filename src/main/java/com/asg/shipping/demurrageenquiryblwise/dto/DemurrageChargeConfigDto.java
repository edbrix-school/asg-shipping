package com.asg.shipping.demurrageenquiryblwise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Demurrage charge configuration - the charge POID mapped on the SHDEMURRAGE global parameter and
 * the tax applicable on it.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DemurrageChargeConfigDto {

	private Long chargePoid;
	private Long taxPoid;
	private BigDecimal taxPercentage;
	/** Y when GLOBAL_TAX_APPLICABLE is enabled for the company. */
	private String taxApplicable;
}
