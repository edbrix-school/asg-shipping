package com.asg.shipping.receipts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReceiptAutoPopulateContainerDto {

	private Long blPoid;
	private String equipmentShipperOwn;
	private String containerNo;
	private LocalDateTime fromDate;
	private LocalDateTime toDate;
	private Long days;
	private BigDecimal demAmount;
	private String equipmentIsoType;
	private Long freeDays;
	private LocalDateTime emptyIn;
}
