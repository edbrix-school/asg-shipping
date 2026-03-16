package com.asg.shipping.bookingFormSH.controller;

import static com.asg.common.lib.dto.response.ApiResponse.error;
import static com.asg.common.lib.dto.response.ApiResponse.success;

import java.util.Map;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.excel.ExcelFileData;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.ExcelExportService;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.bookingFormSH.dto.BookingFormCreateDTO;
import com.asg.shipping.bookingFormSH.dto.BookingFormDto;
import com.asg.shipping.bookingFormSH.dto.BookingFormUpdateDTO;
import com.asg.shipping.bookingFormSH.service.BookingFormService;
import com.asg.shipping.portMaster.dto.PortMasterResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/v1/booking-form-sh")
@RequiredArgsConstructor
@Slf4j
public class BookingFormController {

	private final BookingFormService bookingFormService;
	private final ExcelExportService excelExportService;
	private final LoggingService loggingService;

	@AllowedAction(UserRolesRightsEnum.VIEW)
	@Operation(summary = "Get all Booking Form SH", description = "Fetches all Booking Form SH records for the given group", responses = {
			@ApiResponse(responseCode = "200", description = "Booking Form records fetched successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PortMasterResponse.class))),
			@ApiResponse(responseCode = "401", description = "Unauthorized") }, security = @SecurityRequirement(name = "bearerAuth"))
	@PostMapping("/search")
	public ResponseEntity<?> searchBookingForm(@ParameterObject Pageable pageable,
			@RequestBody(required = false) FilterRequestDto filters) {
		log.info("Search request for Booking Form with docId: {}", UserContext.getDocumentId());
		Map<String, Object> result = bookingFormService.searchBookingForm(UserContext.getDocumentId(), filters,
				pageable);
		return success("Booking Form records retrieved successfully", result);
	}

	@AllowedAction(UserRolesRightsEnum.VIEW)
	@Operation(summary = "Get Booking Form by POID", description = "Fetches a specific Booking Form by POID", responses = {
			@ApiResponse(responseCode = "200", description = "Booking Form retrieved successfully.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PortMasterResponse.class))),
			@ApiResponse(responseCode = "404", description = "Booking Form not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized") }, security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/{id}")
	public ResponseEntity<?> getBookingForm(
			@Parameter(description = "Transaction POID", required = true, example = "5001") @PathVariable Long id) {
		log.info("Get request for Booking Form with id: {}", id);
		BookingFormDto dto = bookingFormService.getBookingForm(id);
		loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), id.toString());
		return success("Booking Form retrieved successfully.", dto);
	}

	@AllowedAction(UserRolesRightsEnum.CREATE)
	@Operation(summary = "Create a new Booking Form", description = "Creates a new Booking Form record for the given group", responses = {
			@ApiResponse(responseCode = "200", description = "Booking Form created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid input"),
			@ApiResponse(responseCode = "401", description = "Unauthorized") }, security = @SecurityRequirement(name = "bearerAuth"))
	@PostMapping
	public ResponseEntity<?> createBookingForm(
			@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated Booking Form details", required = true) @Valid @RequestBody BookingFormCreateDTO createDTO) {
		log.info("Create request for Booking Form");
		BookingFormDto dto = bookingFormService.createBookingForm(createDTO);
		return success("Booking Form created successfully", dto);
	}

	@AllowedAction(UserRolesRightsEnum.EDIT)
	@Operation(summary = "Update an existing Booking Form", description = "Updates the Booking Form details for the given Transaction POID", responses = {
			@ApiResponse(responseCode = "200", description = "Booking Form updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PortMasterResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid input"),
			@ApiResponse(responseCode = "404", description = "Booking Form not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized") }, security = @SecurityRequirement(name = "bearerAuth"))
	@PutMapping("/{id}")
	public ResponseEntity<?> updateBookingForm(
			@Parameter(description = "Transaction POID", required = true, example = "5001") @PathVariable Long id,
			@Valid @RequestBody BookingFormUpdateDTO updateDTO) {
		log.info("Update request for Booking Form with id: {}", id);
		bookingFormService.updateBookingForm(id, updateDTO);
		BookingFormDto dto = bookingFormService.getBookingForm(id);
		return success("Booking Form updated successfully", dto);
	}

	@AllowedAction(UserRolesRightsEnum.DELETE)
	@Operation(summary = "Delete a Booking Form", description = "Soft deletes a Booking Form record", responses = {
			@ApiResponse(responseCode = "200", description = "Booking Form deleted successfully"),
			@ApiResponse(responseCode = "404", description = "Booking Form not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized") }, security = @SecurityRequirement(name = "bearerAuth"))
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteBookingForm(
			@Parameter(description = "Transaction POID", required = true, example = "5001") @PathVariable Long id) {
		log.info("Delete request for Booking Form with id: {}", id);
		bookingFormService.deleteBookingForm(id);
		loggingService.createLogSummaryEntry(LogDetailsEnum.DELETED, UserContext.getDocumentId(), id.toString());
		return success("Booking Form deleted successfully");
	}

	@AllowedAction(UserRolesRightsEnum.VIEW)
	@Operation(summary = "Generate COPRAR Booking Form", description = "Generate COPRAR Booking Form", responses = {
			@ApiResponse(responseCode = "200", description = "Generate COPRAR Booking Form successfully"),
			@ApiResponse(responseCode = "401", description = "Unauthorized") }, security = @SecurityRequirement(name = "bearerAuth"))
	@PostMapping("/{id}/generate-coprar")
	public ResponseEntity<?> generateCoprarBooking(
			@Parameter(description = "Transaction POID", required = true, example = "5001") @PathVariable Long id) {
		log.info("Generate COPRAR booking file request for transaction: {}", id);
		String status = bookingFormService.generateCoprarBooking(id);
		if (status.toLowerCase().startsWith("error"))
			return error(status, 400);
		return success(status);
	}

	@AllowedAction(UserRolesRightsEnum.VIEW)
	@Operation(summary = "Get empty shipper Booking Form", description = "Get empty shipper Booking Form", responses = {
			@ApiResponse(responseCode = "200", description = "Get empty shipper Booking Form successfully"),
			@ApiResponse(responseCode = "401", description = "Unauthorized") }, security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/empty-shipper")
	public ResponseEntity<?> getEmptyShipper(
			@Parameter(description = "Company POID", required = true, example = "5001") @RequestParam Long companyPoid) {
		log.info("Get empty shipper request for company: {}", companyPoid);
		String shipperPoid = bookingFormService.getEmptyShipper(companyPoid);
		return success("Empty shipper retrieved successfully", shipperPoid);
	}

	@AllowedAction(UserRolesRightsEnum.EDIT)
	@Operation(summary = "Get empty shipper Booking Form", description = "Get empty shipper Booking Form", responses = {
			@ApiResponse(responseCode = "200", description = "Get empty shipper Booking Form successfully"),
			@ApiResponse(responseCode = "401", description = "Unauthorized") }, security = @SecurityRequirement(name = "bearerAuth"))
	@PostMapping("/{id}/process-empty-container-load")
	public ResponseEntity<?> processEmptyContainerLoad(
			@Parameter(description = "Transaction POID", required = true, example = "5001") @PathVariable Long id) {
		log.info("Process empty container load request for transaction: {}", id);
		String status = bookingFormService.processEmptyContainerLoad(id);
		if (status.toLowerCase().startsWith("error"))
			return error(status, 500);
		return success(status);
	}

	@AllowedAction(UserRolesRightsEnum.PRINT)
	@Operation(summary = "Generate Excel for VgmCustXLGenerateXL")
	@GetMapping("/excel/vgmCustXLGenerateXL/{transactionPoid}")
	public ResponseEntity<?> exportExcel(
			@Parameter(description = "Transaction POID", example = "476") @PathVariable Long transactionPoid) {
		try {

			ExcelFileData data = excelExportService.generateExcel("100-311", String.valueOf(transactionPoid), null,
					"VGMCustXLFile.xlsx");
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + data.getFileName())
					.contentType(MediaType.APPLICATION_OCTET_STREAM).body(data.getContent());
		} catch (Exception e) {
			log.error("Failed to generate Excel", e);
			return error("Failed to generate Excel: " + e.getMessage(), 500);
		}
	}

	@AllowedAction(UserRolesRightsEnum.PRINT)
	@GetMapping("/mateBookingPrintForm/{transactionPoid}")
	public ResponseEntity<?> print(
			@Parameter(description = "Transaction POID", example = "21") @PathVariable Long transactionPoid) {
		try {
			byte[] pdf = bookingFormService.mateBookingPrintForm(transactionPoid);
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION,
							"attachment; filename=container-mate-receipts-" + transactionPoid + ".pdf")
					.contentType(MediaType.APPLICATION_PDF).body(pdf);
		} catch (Exception e) {
			log.error("Failed to generate PDF for Banking Form SH: {}", transactionPoid, e);
			return error("Failed to generate PDF: " + e.getMessage(), 500);
		}
	}

	@AllowedAction(UserRolesRightsEnum.PRINT)
	@GetMapping("/empty-release/{transactionPoid}")
	public ResponseEntity<?> cntEmptyBookingPrintForm(
			@Parameter(description = "Transaction POID", example = "21") @PathVariable Long transactionPoid) {
		try {
			byte[] pdf = bookingFormService.cntEmptyBookingPrintForm(transactionPoid);
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION,
							"attachment; filename=container-empty-release-" + transactionPoid + ".pdf")
					.contentType(MediaType.APPLICATION_PDF).body(pdf);
		} catch (Exception e) {
			log.error("Failed to generate PDF for Banking Form SH: {}", transactionPoid, e);
			return error("Failed to generate PDF: " + e.getMessage(), 500);
		}
	}

	@AllowedAction(UserRolesRightsEnum.PRINT)
	@GetMapping("/all-container/{transactionPoid}")
	public ResponseEntity<?> cntReturnBookingPrintFormAll(
			@Parameter(description = "Transaction POID", example = "21") @PathVariable Long transactionPoid,
			@Parameter(description = "Print Stamp", example = "Y") @RequestParam String printStamp) {
		try {
			byte[] pdf = bookingFormService.cntReturnBookingPrintFormAll(transactionPoid, printStamp);
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION,
							"attachment; filename=all-container-" + transactionPoid + ".pdf")
					.contentType(MediaType.APPLICATION_PDF).body(pdf);
		} catch (Exception e) {
			log.error("Failed to generate PDF for Banking Form SH: {}", transactionPoid, e);
			return error("Failed to generate PDF: " + e.getMessage(), 500);
		}
	}

}
