package com.asg.shipping.dayCloseShiping.dto;

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
	private Long chequeCount;
}
