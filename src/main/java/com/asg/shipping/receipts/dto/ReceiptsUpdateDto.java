package com.asg.shipping.receipts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReceiptsUpdateDto {

	@Schema(description = "Document Reference", example = "RCP-2025-001")
	private String docRef;

	@Schema(description = "Transaction Date", example = "2025-01-15")
	private LocalDateTime transactionDate;

	@Schema(description = "BL POID", example = "1001")
	private Long blPoid;

	@Schema(description = "Company POID", example = "100")
	private Long companyPoid;

	@Schema(description = "Release Type", example = "ORIGINAL")
	private String releaseType;

	@Schema(description = "Print DO Customer POID", example = "5001")
	private Long printDoCustomerPoid;

	@Schema(description = "Cheque Company POID", example = "200")
	private Long chequeCompany;

	@Schema(description = "CPR/ID Person", example = "123456789")
	private String cpr;

	@Schema(description = "Released To Person Name", example = "John Doe")
	private String name;

	@Schema(description = "Released Address Person", example = "123 Main St, City")
	private String contact;

	@Schema(description = "Payment Reference", example = "CHQ-2025-001")
	private String paymentReference;

	@Schema(description = "Remarks", example = "Receipt for BL shipment")
	private String remarks;

	@Schema(description = "Token Number", example = "12345")
	private Long token;

	@Schema(description = "Receipt Amount", example = "5000.00")
	private BigDecimal rcptAmount;

	@Schema(description = "Receipt Type", example = "CASH")
	private String rcptType;

	@Schema(description = "Container Details")
	private List<ReceiptContainerDto> container;

	@Schema(description = "Charges Details")
	private List<ReceiptCharges> charges;

	@Schema(description = "Payment Details")
	private List<ReceiptPaymentDetailDto> paymentDetail;
}
