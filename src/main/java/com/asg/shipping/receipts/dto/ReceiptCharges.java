package com.asg.shipping.receipts.dto;

import com.asg.common.lib.dto.LovGetListDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReceiptCharges {

	@Schema(description = "Action Type: ISCREATED, ISUPDATED, ISDELETED, NOCHANGES", example = "ISCREATED")
	private String actionType;

	private Long detRowId;
	private Long blPoid;
	private LovGetListDto blDet;
	private Long chargePoid;
	private LovGetListDto chargeDet;
	private BigDecimal amount;
	private BigDecimal taxPercentage;
	private BigDecimal taxAmount;
	private Long taxPoid;
	private LovGetListDto taxDet;

	@Schema(description = "Amount Select flag: Y or N", example = "Y")
	private String amountSelect;
}
