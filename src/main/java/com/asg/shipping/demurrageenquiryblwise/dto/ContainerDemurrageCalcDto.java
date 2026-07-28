package com.asg.shipping.demurrageenquiryblwise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Demurrage recalculated for a single container up to the applied To Date.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContainerDemurrageCalcDto {

	private LocalDate fromDate;
	private LocalDate toDate;
	private Long days;
	private BigDecimal amount;
}
