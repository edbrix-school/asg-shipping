package com.asg.shipping.shippingofoqv2.controller;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.shippingofoqv2.dto.*;
import com.asg.shipping.shippingofoqv2.service.ShippingOFOQV2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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

import static com.asg.common.lib.dto.response.ApiResponse.internalServerError;
import static com.asg.common.lib.dto.response.ApiResponse.success;

@RestController
@RequestMapping("v1/shipping-ofoq-v2")
@Slf4j
@RequiredArgsConstructor
public class ShippingOFOQV2Controller {

    private final ShippingOFOQV2Service shippingOFOQV2Service;


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
                    content = @Content(schema = @Schema(implementation = OFOQManifestStatusResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> saveDocument(
            @Valid @RequestBody ShippingOFOQV2Request request) {

        try {
            OFOQCheckStatusResponseDto response = shippingOFOQV2Service.createShippingOFOQ(request);

            return success("Document saved and submitted successfully", response);


        } catch (Exception e) {
            return internalServerError("internal server" + e.getMessage() + e);
        }
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
            @Parameter(description = "Document identifier", required = true, example = "OFOQ-001")
            @RequestParam String documentId,

            @RequestBody(required = false) FilterRequestDto filters,

            @Parameter(description = "Start date for filtering (YYYY-MM-DD)")
            @RequestParam(required = false) LocalDate startDate,

            @Parameter(description = "End date for filtering (YYYY-MM-DD)")
            @RequestParam(required = false) LocalDate endDate,

            @PageableDefault(size = 20) Pageable pageable
    ) {
        Map<String, Object> result = shippingOFOQV2Service.listShippingOFOQ(documentId, filters, startDate, endDate, pageable);
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


	@Operation(
			summary = "Check OFOQ Status",
			description = "Check the current status of an OFOQ document"
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "OFOQ status check request",
			required = true,
			content = @Content(schema = @Schema(implementation = OFOQCheckStatusDto.class))
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully checked OFOQ status",
				content = @Content(schema = @Schema(implementation = OFOQCheckStatusResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request parameters"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@PostMapping("/check-status")
	public ResponseEntity<?> checkStatus(
			@Valid @RequestBody OFOQCheckStatusDto request
	) {
		try {
			OFOQCheckStatusResponseDto response = shippingOFOQV2Service.checkStatus(request);
			return success("Status checked successfully", response);
		} catch (Exception e) {
			log.error("Error checking OFOQ status", e);
			return internalServerError("Error checking status: " + e.getMessage());
		}
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
					content = @Content(schema = @Schema(implementation = OFOQManifestStatusResponseDto.class))),
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
		try {
            OFOQCheckStatusResponseDto response = shippingOFOQV2Service.updateShippingOFOQ(transactionPoid, request);
			return success("Document updated successfully", response);
		} catch (Exception e) {
			return internalServerError("Error updating document: " + e.getMessage());
		}
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
			@PathVariable Long transactionPoid
	) {
		try {
			shippingOFOQV2Service.deleteShippingOFOQ(transactionPoid);
			return success("Document deleted successfully", null);
		} catch (Exception e) {
			return internalServerError("Error deleting document: " + e.getMessage());
		}
	}
}