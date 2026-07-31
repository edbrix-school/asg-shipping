package com.asg.shipping.daycloseshiping.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class DayCloseSummaryProjectionImpl implements DayCloseSummaryProjection {

	private LocalDate transactionDate;
	private BigDecimal cashAmount;
	private BigDecimal chequeAmount;
	private BigDecimal totalAmount;
	private Long noOfCheques;
	private String verifiedRcvd;
	private String mainOfcRemarks;
}
