package com.asg.shipping.shippingofoqv2.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.shippingofoqv2.dto.*;
import com.asg.shipping.shippingofoqv2.service.ShippingOFOQV2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import static com.asg.common.lib.dto.response.ApiResponse.success;

@RestController
@RequestMapping("v1/shipping-ofoq-v2")
@Slf4j
@RequiredArgsConstructor
public class ShippingOFOQV2Controller {

	private final ShippingOFOQV2Service shippingOFOQV2Service;
	private final LoggingService loggingService;


	@PostMapping
	@Operation(summary = "Save OFOQ Document and Submit to External API",
			description = "Create and save a new OFOQ document, then submit it to external API")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "OFOQ document request details",
			required = true,
			content = @Content(schema = @Schema(implementation = ShippingOFOQV2Request.class))
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Document saved and submitted successfully",
					content = @Content(schema = @Schema(implementation = OFOQCheckStatusResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request parameters"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	public ResponseEntity<?> saveDocument(
			@Valid @RequestBody ShippingOFOQV2Request request) {

		OFOQCheckStatusResponseDto response = shippingOFOQV2Service.createShippingOFOQ(request);

		return success("Document saved and submitted successfully", response);
	}

	@Operation(
			summary = "Get OFOQ API Data by ID",
			description = "Retrieve specific OFOQ API data record by transaction POID"
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully retrieved OFOQ API data",
					content = @Content(schema = @Schema(implementation = OFOQVoyageDataResponse.class))),
			@ApiResponse(responseCode = "404", description = "OFOQ API data not found"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@GetMapping("/{transactionPoid}")
	public ResponseEntity<?> getById(
			@Parameter(description = "Transaction POID", required = true)
			@PathVariable Long transactionPoid
	) {
		OFOQVoyageDataResponse result = shippingOFOQV2Service.getShippingOFOQById(transactionPoid);
		loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
		return success("OFOQ API data retrieved successfully", result);
	}

	@Operation(
			summary = "List OFOQ API Data",
			description = "Retrieve a paginated list of OFOQ API data with optional filtering and date range"
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully retrieved OFOQ data list",
					content = @Content(schema = @Schema(implementation = Map.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request parameters"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@PostMapping("/list")
	public ResponseEntity<?> list(

			@RequestBody(required = false) FilterRequestDto filters,

			@Parameter(description = "Start date for filtering (YYYY-MM-DD)")
			@RequestParam(required = false) LocalDate startDate,

			@Parameter(description = "End date for filtering (YYYY-MM-DD)")
			@RequestParam(required = false) LocalDate endDate,

			@PageableDefault(size = 20) Pageable pageable
	) {
		Map<String, Object> result = shippingOFOQV2Service.listShippingOFOQ(UserContext.getDocumentId(), filters, startDate, endDate, pageable);
		return success("OFOQ API data retrieved successfully", result);
	}


	@Operation(
			summary = "Load OFOQ Details",
			description = "Load voyage jobs and BLs into detail tables via stored procedure"
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "OFOQ load details request",
			required = true,
			content = @Content(schema = @Schema(implementation = LoadOFOQDetailsRequest.class))
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully loaded OFOQ details",
					content = @Content(schema = @Schema(implementation = OFOQLoadItemDetailsResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request parameters"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@PostMapping("/load-details")
	public ResponseEntity<?> loadOFOQDetails(
			@RequestBody LoadOFOQDetailsRequest request
	) {
		OFOQLoadItemDetailsResponse result = shippingOFOQV2Service.loadOFOQDetails(request);
		return success("OFOQ details loaded successfully", result);
	}


	@PostMapping("/check-status")
	@Operation(
			summary = "Check OFOQ Status",
			description = "Check the current status of an OFOQ document"
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "OFOQ status check request",
			required = true,
			content = @Content(
					mediaType = "application/json",
					examples = @ExampleObject(
							name = "Check Status Request",
							summary = "Check OFOQ document status",
							value = """
									{
									  "functionalReference": "MSTR250128",
									  "transactionPoid": 37284,
									  "docReference": "ASG00006"
									}
									"""
					)
			)
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Successfully checked OFOQ status",
					content = @Content(
							mediaType = "application/json",
							examples = @ExampleObject(
									name = "Check Status Success Response",
									summary = "OFOQ status with manifest responses",
									value = """
											{
											  "message": "Status checked successfully",
											  "statusCode": 200,
											  "success": true,
											  "result": {
											    "data": {
											      "header": {
											        "transactionPoid": 37953,
											        "docRef": "ASG00115",
											        "transactionDate": "2026-02-05T15:08:27",
											        "voyageNo": "442",
											        "vesselPoid": 27961,
											        "arrivalDate": "2024-10-18T00:00:00",
											        "rotationNumber": 240000495045646,
											        "apiProvisionalMfNo": null,
											        "apiProvisionalStatus": "FAILED",
											        "manifestNo": "NIL",
											        "manifestStatus": "NIL",
											        "remarks": "Test OFOQ creation 234",
											        "functionalReference": "MSTR260088",
											        "deleted": "N",
											        "createdBy": "DEVUSER2",
											        "createdDate": "2026-02-05T15:08:27.682237",
											        "lastModifiedBy": "DEVUSER2",
											        "lastModifiedDate": "2026-02-05T16:20:41.214516"
											      },
											      "manifestResponses": [
											        {
											          "transactionPoid": 37953,
											          "detRowId": 1,
											          "date": "2026-02-05T12:38:33",
											          "functionalReference": "MSTR260088",
											          "StatusCode": "200 OK",
											          "responseMessage": "Manifest Consignment is Mandatory ",
											          "processingStatus": "FAILED"
											        },
											        {
											          "transactionPoid": 37953,
											          "detRowId": 2,
											          "date": "2026-02-05T13:31:50",
											          "functionalReference": "MSTR260088",
											          "StatusCode": "200 OK",
											          "responseMessage": "Manifest Consignment is Mandatory ",
											          "processingStatus": "FAILED"
											        }
											      ]
											    }
											  }
											}
											"""
							)
					)
			),
			@ApiResponse(responseCode = "400", description = "Invalid request parameters"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	public ResponseEntity<?> checkStatus(
			@Valid @RequestBody OFOQCheckStatusDto request
	) {
		OFOQCheckStatusResponseDto response =
				shippingOFOQV2Service.checkStatus(request, "M");

		return success("Status checked successfully", response);
	}


	@Operation(
			summary = "Update OFOQ Document",
			description = "Update an existing OFOQ document and submit to external API"
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "OFOQ document update request",
			required = true,
			content = @Content(schema = @Schema(implementation = ShippingOFOQV2Request.class))
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Document updated successfully",
					content = @Content(schema = @Schema(implementation = OFOQCheckStatusResponseDto.class))),
			@ApiResponse(responseCode = "404", description = "Document not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request parameters"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@PutMapping("/{transactionPoid}")
	public ResponseEntity<?> updateDocument(
			@Parameter(description = "Transaction POID", required = true)
			@PathVariable Long transactionPoid,
			@Valid @RequestBody ShippingOFOQV2Request request
	) {
		OFOQCheckStatusResponseDto response = shippingOFOQV2Service.updateShippingOFOQ(transactionPoid, request);
		return success("Document updated successfully", response);

	}

	@Operation(
			summary = "Delete OFOQ Document",
			description = "Soft delete an OFOQ document (mark as deleted)"
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Document deleted successfully"),
			@ApiResponse(responseCode = "404", description = "Document not found"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@DeleteMapping("/{transactionPoid}")
	public ResponseEntity<?> deleteDocument(
			@Parameter(description = "Transaction POID", required = true)
			@PathVariable Long transactionPoid, @Valid @RequestBody DeleteReasonDto deleteReasonDto
	) {

		shippingOFOQV2Service.deleteShippingOFOQ(transactionPoid, deleteReasonDto);
		return success("Document deleted successfully", null);

	}

	@Operation(
			summary = "Amend Bill of Lading",
			description = "Amend an existing Bill of Lading in the OFOQ document"
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "Bill of Lading amendment request",
			required = true,
			content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = OFOQAmendBlRequestDto.class),
					examples = @ExampleObject(
							name = "Amend BL Request",
							value = """
									{
									  "transactionPoid": 37967,
									  "blNumber": "CSS24MUNBAH048883",
									  "functionalReference": "MSTR260101",
									  "docReference": "ASG00001",
									  "vesselPoid": 27961
									}
									"""
					)
			)
	)
	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "Bill of Lading amended successfully",
					content = @Content(
							mediaType = "application/json",
							examples = @ExampleObject(
									name = "Success Response",
									value = """
											{
											  "success": true,
											  "result": {
											    "data": {
											      "amendBL": {
											        "detRowId": 1,
											        "blNumber": "CSS24MUNBAH048883",
											        "functionalReference": "MSTR260103",
											        "statusCode": 200,
											        "response": "Accepted",
											        "processingStatus": null,
											        "amendmentRequest": null,
											        "manifestStatus": null
											      },
											      "manifestAmendmentResponses": []
											    }
											  },
											  "statusCode": 200,
											  "message": "Amend bl successfully"
											}
											"""
							)
					)
			),
			@ApiResponse(responseCode = "400", description = "Invalid request parameters"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@PostMapping("/amend-bl")
	public ResponseEntity<?> amendBl(@Valid @RequestBody OFOQAmendBlRequestDto request) {

		AmendBlDto response = shippingOFOQV2Service.amendBl(request);
		return success("Amend bl successfully", response);
	}

}