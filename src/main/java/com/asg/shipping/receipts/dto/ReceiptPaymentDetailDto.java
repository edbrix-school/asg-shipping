package com.asg.shipping.receipts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class ReceiptPaymentDetailDto {

	@Schema(description = "Action Type: ISCREATED, ISUPDATED, ISDELETED, NOCHANGES", example = "ISCREATED")
	private String actionType;

	private Long detRowId;
	private String pymtType;
	private BigDecimal amount;
	private Long ttBankPoid;
	private String chqCardno;
	private LocalDate chqDate;
	private String accountName;
	private String accountNo;
	private Long bankPoid;
}
