package com.asg.shipping.receipts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptContainerDto {

	@Schema(description = "Action Type: ISCREATED, ISUPDATED, ISDELETED, NOCHANGES", example = "ISCREATED")
	private String actionType;

	private Long detRowId;
	private String containerSocYn;
	private Long blPoid;
	private String containerNo;
	private String equipmentIsoType;
	private Long freeDays;
	private LocalDateTime dmFrmDate;
	private LocalDateTime dmToDate;
	private Long dmDays;
	private BigDecimal dmChargeAmt;
	private BigDecimal cntTaxPercentage;
	private BigDecimal cntTaxAmount;
	private LocalDateTime emptyIn;
	private Long cntTaxPoid;
}
