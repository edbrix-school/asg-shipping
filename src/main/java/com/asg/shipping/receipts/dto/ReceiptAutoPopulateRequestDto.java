package com.asg.shipping.receipts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReceiptAutoPopulateRequestDto {

	@NotNull(message = "BL POID is required")
	@Schema(description = "Bill of Lading POID", example = "1001", required = true)
	private Long blPoid;

	@Schema(description = "Transaction POID (null for create, required for update)", example = "5001")
	private Long transactionPoid;
}
