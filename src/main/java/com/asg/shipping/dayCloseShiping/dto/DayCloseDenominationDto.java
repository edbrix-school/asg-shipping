package com.asg.shipping.dayCloseShiping.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DayCloseDenominationDto {

	private Long detRowId;
	private String currencyType;
	private BigDecimal denomination;
	private Long noOfTran;
	private BigDecimal cashAmount;
}
