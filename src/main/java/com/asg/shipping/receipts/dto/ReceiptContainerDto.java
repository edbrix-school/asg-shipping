package com.asg.shipping.receipts.dto;

import com.asg.common.lib.dto.LovGetListDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

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
	private LovGetListDto blDet;
	private String containerNo;
	private String equipmentIsoType;
	private Long freeDays;
	private LocalDate dmFrmDate;
	private LocalDate dmToDate;
	private Long dmDays;
	private BigDecimal dmChargeAmt;
	private BigDecimal cntTaxPercentage;
	private BigDecimal cntTaxAmount;
	private LocalDate emptyIn;
	private Long cntTaxPoid;
	private LovGetListDto taxDet;
}
