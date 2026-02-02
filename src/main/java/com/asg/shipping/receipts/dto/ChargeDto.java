package com.asg.shipping.receipts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChargeDto {
	private String chargeTypeApplicable;
	private String chargeApplicable;
	private Long chargeCodePoid;
	private BigDecimal amount20;
	private BigDecimal amount40;
	private BigDecimal amountOther;
	private Long taxPoid;
	private BigDecimal taxPercentage;
	private String taxApplicable;
}
