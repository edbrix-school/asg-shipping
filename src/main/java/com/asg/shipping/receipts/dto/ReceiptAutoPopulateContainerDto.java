package com.asg.shipping.receipts.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReceiptAutoPopulateContainerDto {

	private Long blPoid;
	private LovGetListDto blDet;
	private String equipmentShipperOwn;
	private String containerNo;
	private LocalDate fromDate;
	private LocalDate toDate;
	private Long days;
	private BigDecimal demAmount;
	private String equipmentIsoType;
	private Long freeDays;
	private LocalDate emptyIn;
}
