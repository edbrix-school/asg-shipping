package com.asg.shipping.receipts.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.receipts.dto.*;
import com.asg.shipping.receipts.enums.ButtonType;
import com.asg.shipping.receipts.service.ReceiptsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@RestController
@RequestMapping("v1/receipts-shipping")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "receipts-controller", description = "Manage Receipts records")
public class ReceiptsController {

	private final ReceiptsService receiptsService;
	private final LoggingService loggingService;

	@AllowedAction(UserRolesRightsEnum.VIEW)
	@GetMapping("/{transactionPoid}")
	@Operation(
			summary = "Get Receipt by ID",
			description = "Retrieve a single Receipt record by its Transaction POID"
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Receipt retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Receipt not found"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	public ResponseEntity<?> getById(
			@Parameter(description = "Transaction POID", required = true, example = "1001")
			@PathVariable Long transactionPoid
	) {

			ReceiptsBlDetailsDto response = receiptsService.getReceipt(transactionPoid);
		loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
		return success("Receipt retrieved successfully", response);

	}

	@AllowedAction(UserRolesRightsEnum.CREATE)
	@PostMapping
	@Operation(
			summary = "Create Receipt",
			description = """
					Create a new Receipt record with container, charges, and payment details.
					
					### Business Rules
					- **Document Reference** is mandatory
					- **Transaction Date** is mandatory
					- **BL POID** is mandatory
					- **Company POID** is mandatory
					"""
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "Receipt creation data",
			required = true,
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ReceiptsCreateDto.class),
					examples = @ExampleObject(
							name = "Receipt Create Example",
							value = """
									{
									  "docRef": "RCP-2025-001",
									  "transactionDate": "2025-01-15",
									  "blPoid": 1001,
									  "companyPoid": 100,
									  "releaseType": "ORIGINAL",
									  "printDoCustomerPoid": 5001,
									  "chequeCompany": 200,
									  "cpr": "123456789",
									  "name": "John Doe",
									  "contact": "123 Main St, City",
									  "paymentReference": "CHQ-2025-001",
									  "remarks": "Receipt for BL shipment",
									  "token": 12345,
									  "rcptAmount": 5000.00,
									  "rcptType": "CASH",
									  "container": [
									    {
									      "containerSocYn": "Y",
									      "blPoid": 1001,
									      "containerNo": "CONT123456",
									      "equipmentIsoType": "20",
									      "freeDays": 5,
									      "dmFrmDate": "2025-01-10",
									      "dmToDate": "2025-01-15",
									      "dmDays": 5,
									      "dmChargeAmt": 500.00,
									      "cntTaxPercentage": 5.00,
									      "cntTaxAmount": 25.00,
									      "emptyIn": "2025-01-20",
									      "cntTaxPoid": 1
									    }
									  ],
									  "charges": [
									    {
									      "blPoid": 1001,
									      "chargePoid": 2001,
									      "amount": 1000.00,
									      "taxPercentage": 5.00,
									      "taxAmount": 50.00,
									      "taxPoid": 1
									    }
									  ],
									  "paymentDetail": [
									    {
									      "pymtType": "CASH",
									      "amount": 5000.00,
									      "ttBankPoid": 3001,
									      "chqCardno": "CHQ123456",
									      "chqDate": "2025-01-15",
									      "accountName": "Main Account",
									      "accountNo": "ACC123456",
									      "bankPoid": 3001
									    }
									  ]
									}
									"""
					)
			)
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Receipt created successfully"),
			@ApiResponse(responseCode = "400", description = "Validation error"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	public ResponseEntity<?> create(
			@Valid @RequestBody ReceiptsCreateDto createDto
	) {
		try {
			ReceiptSaveResponseDto response = receiptsService.createReceipt(createDto);
			return success("Receipt created successfully", response);
		} catch (Exception e) {
			return internalServerError("Failed to create Receipt: " + e.getMessage());
		}
	}

	@AllowedAction(UserRolesRightsEnum.EDIT)
	@PutMapping("/{transactionPoid}")
	@Operation(
			summary = "Update Receipt",
			description = """
					Update an existing Receipt record with container, charges, and payment details.
					
					### Business Rules
					- Cannot update deleted records
					- All detail records will be replaced
					"""
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "Receipt update data",
			required = true,
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ReceiptsUpdateDto.class),
					examples = @ExampleObject(
							name = "Receipt Update Example",
							value = """
									{
									  "docRef": "RCP-2025-001-UPD",
									  "transactionDate": "2025-01-16",
									  "blPoid": 1001,
									  "companyPoid": 100,
									  "releaseType": "DUPLICATE",
									  "printDoCustomerPoid": 5001,
									  "chequeCompany": 200,
									  "cpr": "123456789",
									  "name": "Jane Doe",
									  "contact": "456 Oak Ave, City",
									  "paymentReference": "CHQ-2025-002",
									  "remarks": "Updated receipt for BL shipment",
									  "token": 12346,
									  "rcptAmount": 5500.00,
									  "rcptType": "CHEQUE",
									  "container": [
									    {
									      "containerSocYn": "N",
									      "blPoid": 1001,
									      "containerNo": "CONT123457",
									      "equipmentIsoType": "40",
									      "freeDays": 7,
									      "dmFrmDate": "2025-01-11",
									      "dmToDate": "2025-01-18",
									      "dmDays": 7,
									      "dmChargeAmt": 700.00,
									      "cntTaxPercentage": 5.00,
									      "cntTaxAmount": 35.00,
									      "emptyIn": "2025-01-22",
									      "cntTaxPoid": 1
									    }
									  ],
									  "charges": [
									    {
									      "blPoid": 1001,
									      "chargePoid": 2002,
									      "amount": 1200.00,
									      "taxPercentage": 5.00,
									      "taxAmount": 60.00,
									      "taxPoid": 1
									    }
									  ],
									  "paymentDetail": [
									    {
									      "pymtType": "CHEQUE",
									      "amount": 5500.00,
									      "ttBankPoid": 3002,
									      "chqCardno": "CHQ123457",
									      "chqDate": "2025-01-20",
									      "accountName": "Secondary Account",
									      "accountNo": "ACC123457",
									      "bankPoid": 3002
									    }
									  ]
									}
									"""
					)
			)
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Receipt updated successfully"),
			@ApiResponse(responseCode = "404", description = "Receipt not found"),
			@ApiResponse(responseCode = "400", description = "Validation error"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	public ResponseEntity<?> update(
			@Parameter(description = "Transaction POID", required = true, example = "1001")
			@PathVariable Long transactionPoid,
			@Valid @RequestBody ReceiptsUpdateDto updateDto
	) {
			ReceiptSaveResponseDto response = receiptsService.updateReceipt(transactionPoid, updateDto);
			return success("Receipt updated successfully", response);
	}

	@AllowedAction(UserRolesRightsEnum.DELETE)
	@DeleteMapping("/{transactionPoid}")
	@Operation(
			summary = "Delete Receipt",
			description = "Soft delete a Receipt record and its associated detail records"
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Receipt deleted successfully"),
			@ApiResponse(responseCode = "404", description = "Receipt not found"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	public ResponseEntity<?> delete(
			@Parameter(description = "Transaction POID", required = true, example = "1001")
			@PathVariable Long transactionPoid, @RequestBody DeleteReasonDto deleteReasonDto
			) {
			receiptsService.deleteReceipt(transactionPoid,deleteReasonDto);
			return success("Receipt deleted successfully", null);
	}

	@AllowedAction(UserRolesRightsEnum.VIEW)
	@Operation(
			summary = "List Receipts",
			description = """
					Fetch Receipt records using filters and pagination.
					
					Valid `searchField` values: DOC_REF, TRANSACTION_POID, RCPT_TYPE, RCPT_AMOUNT
					"""
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			content = @Content(
					examples = @ExampleObject(
							name = "Receipt Filters",
							value = """
									{
									  "operator": "AND",
									  "isDeleted": "N",
									  "filters": [
									    {
									      "searchField": "DOC_REF",
									      "searchValue": "RCP-2025"
									    },
									    {
									      "searchField": "RCPT_TYPE",
									      "searchValue": "CASH"
									    }
									  ]
									}
									"""
					)
			)
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Receipt list retrieved successfully"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@PostMapping("/list")
	public ResponseEntity<?> list(
			@ParameterObject Pageable pageable,
			@RequestBody(required = false) FilterRequestDto filters
	) {
			Map<String, Object> response = receiptsService.list(filters, pageable);
			return success("Receipt list retrieved successfully", response);
	}

	@AllowedAction(UserRolesRightsEnum.VIEW)
	@GetMapping("/autopopulate")
	@Operation(
			summary = "Auto Populate Receipt Fields",
			description = """
					Auto-populate receipt fields based on BL POID.
					
					### Usage
					- **For Create**: Pass `transactionPoid` as null or omit it
					- **For Update**: Pass existing `transactionPoid` to exclude current receipt from duplicate checks
					
					Returns BL details, available charges, and container demurrage information.
					"""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Fields auto-populated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid BL POID"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	public ResponseEntity<?> autoPopulateFields(
			@Parameter(description = "BL POID", required = true) @RequestParam Long blPoid,
			@Parameter(description = "Optional Transaction POID (for update mode)") @RequestParam(required = false) Long transactionPoid
	) {
			ReceiptAutoPopulateDto dto = receiptsService.autoPopulateFields(blPoid, transactionPoid);
			return success("Successfully auto-populated the fields", dto);

	}

	@AllowedAction(UserRolesRightsEnum.VIEW)
	@PostMapping("/calculate-demurrage")
	@Operation(
			summary = "Calculate Demurrage Amount with Charges and Tax",
			description = """
					Calculate demurrage amount with tax, late collection charges, and revalidation charges.
					
					### Returns
					- Demurrage amount with tax calculation
					- Late collection charges (if BL not invoiced)
					- Revalidation charges (if BL already invoiced)
					- Total amount with all charges and taxes
					"""
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "Demurrage calculation request",
			required = true,
			content = @Content(
					mediaType = "application/json",
					examples = {
							@ExampleObject(
									name = "Create Receipt - Calculate Demurrage",
									description = "For new receipt creation, pass transactionPoid as null",
									value = """
											{
											  "transactionPoid": null,
											  "blPoid": 1001,
											  "containerNo": "CONT123456",
											  "containerType": 1,
											  "fromDate": "2025-01-01",
											  "toDate": "2025-01-15",
											  "extraFreeDays": 0
											}
											"""
							),
							@ExampleObject(
									name = "Update Receipt - Calculate Demurrage",
									description = "For existing receipt update, pass the transaction POID",
									value = """
											{
											  "transactionPoid": 5001,
											  "blPoid": 1001,
											  "containerNo": "CONT123456",
											  "containerType": 1,
											  "fromDate": "2025-01-01",
											  "toDate": "2025-01-20",
											  "extraFreeDays": 0
											}
											"""
							)
					}
			)
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Demurrage calculated successfully with charges and tax"),
			@ApiResponse(responseCode = "400", description = "Invalid request parameters"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	public ResponseEntity<?> calculateDemurrage(
			@Valid @RequestBody ReceiptCalculateDemurrageRequestDto requestDto
	) {
			ReceiptCalculateDemurrageResponseDto response = receiptsService.calculateDemurrage(requestDto);
			return success("Demurrage calculated successfully", response);

	}

	@AllowedAction(UserRolesRightsEnum.PRINT)
	@GetMapping("/receipt-invoice/{transactionPoid}")
	public ResponseEntity<?> receiptAndInvoicePrint(
			@Parameter(description = "Transaction POID", example = "12345")
			@PathVariable Long transactionPoid,
			@Parameter(description = "BL POID", example = "67890")
			@RequestParam Long blPoid,
			@RequestParam ButtonType buttonType
	) {
		try {
			byte[] pdf = receiptsService.receiptAndInvoicePrint(transactionPoid, blPoid,buttonType);
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION,
							"attachment; filename=receipts(shipping)" + buttonType.name().toLowerCase() + "-" +  transactionPoid + ".pdf")
					.contentType(MediaType.APPLICATION_PDF)
					.body(pdf);
		} catch (Exception e) {
			log.error("error",e);
			return error("Failed to generate PDF: " + e.getMessage(), 500);
		}
	}

}