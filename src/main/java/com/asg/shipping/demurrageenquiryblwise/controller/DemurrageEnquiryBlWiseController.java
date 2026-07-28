package com.asg.shipping.demurrageenquiryblwise.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.shipping.demurrageenquiryblwise.dto.DemurrageEnquiryRequestDto;
import com.asg.shipping.demurrageenquiryblwise.dto.DemurrageEnquiryResponseDto;
import com.asg.shipping.demurrageenquiryblwise.service.DemurrageEnquiryBlWiseService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.badRequest;
import static com.asg.common.lib.dto.response.ApiResponse.error;
import static com.asg.common.lib.dto.response.ApiResponse.internalServerError;
import static com.asg.common.lib.dto.response.ApiResponse.success;

/**
 * Demurrage Enquiry - BL wise (Shipping -> Transactions, 100-144). The document is display only:
 * nothing is stored, every figure is derived from the BL, the line tariff and the charge masters.
 */
@RestController
@RequestMapping("v1/demurrage-enquiry-bl-wise")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "demurrage-enquiry-bl-wise-controller", description = "Demurrage enquiry of a BL, container wise")
public class DemurrageEnquiryBlWiseController {

	private final DemurrageEnquiryBlWiseService demurrageEnquiryBlWiseService;

	@AllowedAction(UserRolesRightsEnum.VIEW)
	@GetMapping("/bl-details")
	@Operation(
			summary = "Load the BL details and its containers",
			description = """
					Loads the containers of the selected BL, as done when a BL is picked on the screen.

					Returns the SOC flag, container number, ISO type, free days, demurrage start date,
					the Empty In date of the container and the delivery order status of the BL.
					The To Date, Days and Amount columns stay empty until `/apply-date` is called.
					"""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "BL details retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "BL not found"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	public ResponseEntity<?> getBlDetails(
			@Parameter(description = "Transaction POID of the BL (LOV ALLBLNUMBER)", required = true, example = "1001")
			@RequestParam Long blPoid
	) {
		DemurrageEnquiryResponseDto response = demurrageEnquiryBlWiseService.getBlDetails(blPoid);
		return success("BL details retrieved successfully", response);
	}

	@AllowedAction(UserRolesRightsEnum.VIEW)
	@PostMapping("/apply-date")
	@Operation(
			summary = "Apply Date - calculate the demurrage and the charges of the BL",
			description = """
					Recalculates the demurrage of every container of the BL up to the given To Date and
					reloads the Charges tab.

					### Business rules
					- **To Date** defaults to the current date when it is not supplied
					- A container that was returned empty is charged only up to its **Empty In** date
					- Containers still inside their free days return zero days and zero amount
					- **Discount (%)** is applied on the calculated demurrage amount of every container
					- The Charges tab holds the BL manifest charges that are neither received nor invoiced,
					  the calculated demurrage (charge mapped on the SHDEMURRAGE parameter) and the
					  late collection / revalidation charges of the port charges tariff
					- **Receipt Amount** is the sum of the charge amounts, **Total (Receipt + VAT) Amount**
					  adds the tax of every charge
					"""
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "Enquiry parameters",
			required = true,
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = DemurrageEnquiryRequestDto.class),
					examples = @ExampleObject(
							name = "Demurrage enquiry",
							value = """
									{
									  "blPoid": 1001,
									  "toDate": "2025-07-28",
									  "discountPercentage": 10
									}
									"""
					)
			)
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Demurrage enquiry calculated successfully"),
			@ApiResponse(responseCode = "400", description = "Validation error"),
			@ApiResponse(responseCode = "404", description = "BL not found"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	public ResponseEntity<?> applyDate(
			@Valid @RequestBody DemurrageEnquiryRequestDto request
	) {
		DemurrageEnquiryResponseDto response = demurrageEnquiryBlWiseService.applyDate(request);
		return success("Demurrage enquiry calculated successfully", response);
	}

	@AllowedAction(UserRolesRightsEnum.PRINT)
	@GetMapping("/demurrage-calculation")
	@Operation(
			summary = "View Demurrage Calculation",
			description = "Demurrage charges tariff calculation of the BL as a PDF (SH/LINE_DEMURRAGE_CALC)."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "PDF generated successfully"),
			@ApiResponse(responseCode = "404", description = "BL not found"),
			@ApiResponse(responseCode = "500", description = "Failed to generate the PDF")
	})
	public ResponseEntity<?> printDemurrageCalculation(
			@Parameter(description = "Transaction POID of the BL", required = true, example = "1001")
			@RequestParam Long blPoid,
			@Parameter(description = "To Date of the calculation, defaults to the current date", example = "2025-07-28")
			@RequestParam(required = false) LocalDate toDate,
			@Parameter(description = "Discount percentage", example = "10")
			@RequestParam(required = false) BigDecimal discountPercentage
	) {
		try {
			byte[] pdf = demurrageEnquiryBlWiseService.printDemurrageCalculation(blPoid, toDate, discountPercentage);
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION,
							"attachment; filename=demurrage-calculation-" + blPoid + ".pdf")
					.contentType(MediaType.APPLICATION_PDF)
					.body(pdf);
		} catch (Exception e) {
			log.error("Failed to generate the demurrage calculation PDF for BL: {}", blPoid, e);
			return error("Failed to generate PDF: " + e.getMessage(), 500);
		}
	}

	@AllowedAction(UserRolesRightsEnum.VIEW)
	@PostMapping("/list")
	@Operation(
			summary = "List of records",
			description = "Fetch the enquiry records using filters and pagination."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "List retrieved successfully"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	public ResponseEntity<?> list(
			@ParameterObject Pageable pageable,
			@RequestBody(required = false) FilterRequestDto filters,
			@RequestParam(required = false) LocalDate startDate,
			@RequestParam(required = false) LocalDate endDate
	) {
		try {
			if ((startDate == null) != (endDate == null)) {
				return badRequest("Both startDate and endDate should be specified or both should be empty.");
			}
			Map<String, Object> response = demurrageEnquiryBlWiseService.list(filters, pageable, startDate, endDate);
			return success("List retrieved successfully", response);
		} catch (Exception e) {
			log.error("Error fetching the demurrage enquiry list", e);
			return internalServerError("Failed to fetch list: " + e.getMessage());
		}
	}
}
