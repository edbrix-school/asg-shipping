package com.asg.shipping.exportManifestUpdate.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.exportManifestUpdate.dto.*;
import com.asg.shipping.exportManifestUpdate.service.ExportManifestBlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.asg.common.lib.dto.response.ApiResponse.success;
import static com.asg.common.lib.dto.response.ApiResponse.error;

/**
 * Controller for Export Manifest Update OPS – BL (100 – 352)
 */
@RestController
@RequestMapping("/v1/export-manifest-update-ops-bl")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class ExportManifestUpdateController {

    private final ExportManifestBlService service;

    // ========== Header Operations ==========

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Get Export BL by ID", description = "Retrieves a single Export BL manifest header with all related information")
    @Parameters({
            @Parameter(
                    name = "X-Document-Id",
                    in = ParameterIn.HEADER,
                    description = "Document identifier required for auditing purposes.",
                    example = "100-352",
                    required = true,
                    schema = @Schema(type = "string", example = "100-352")
            ),
            @Parameter(
                    name = "X-Action-Requested",
                    in = ParameterIn.HEADER,
                    description = "Action requested must match this endpoint's @AllowedAction (VIEW).",
                    example = "VIEW",
                    required = true,
                    schema = @Schema(type = "string", example = "VIEW")
            )
    })
    @GetMapping("/{transactionPoid}")
    public ResponseEntity<?> getExportBlById(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
        return success("Export BL retrieved successfully", service.getExportBlById(transactionPoid));
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Search Export BLs", description = "Searches and retrieves a paginated list of Export BL manifests")
    @Parameters({
            @Parameter(
                    name = "X-Document-Id",
                    in = ParameterIn.HEADER,
                    description = "Document identifier required for auditing purposes.",
                    example = "100-352",
                    required = true,
                    schema = @Schema(type = "string", example = "100-352")
            ),
            @Parameter(
                    name = "X-Action-Requested",
                    in = ParameterIn.HEADER,
                    description = "Action requested must match this endpoint's @AllowedAction (VIEW).",
                    example = "VIEW",
                    required = true,
                    schema = @Schema(type = "string", example = "VIEW")
            )
    })
    @PostMapping("/list")
    public ResponseEntity<?> searchExportBls(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters) {
        return success("Export BL list fetched successfully", service.searchExportBls(filters, pageable));
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @Operation(summary = "Create Export BL", description = "Creates a new Export BL manifest header")
    @Parameters({
            @Parameter(
                    name = "X-Document-Id",
                    in = ParameterIn.HEADER,
                    description = "Document identifier required for auditing purposes.",
                    example = "100-352",
                    required = true,
                    schema = @Schema(type = "string", example = "100-352")
            ),
            @Parameter(
                    name = "X-Action-Requested",
                    in = ParameterIn.HEADER,
                    description = "Action requested must match this endpoint's @AllowedAction (CREATE).",
                    example = "CREATE",
                    required = true,
                    schema = @Schema(type = "string", example = "CREATE")
            )
    })
    @PostMapping
    public ResponseEntity<?> createExportBl(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Export BL details", required = true)
            @Valid @RequestBody ExportManifestBlRequest request) {
        return success("Export BL created successfully", service.createExportBl(request));
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{id}")
    @Operation(
            summary = "Update Export BL and Details",
            description = "Updates an existing Export BL manifest header and related details (general cargo, containers, cargo description, marks, charges)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
            @Parameter(
                    name = "X-Document-Id",
                    in = ParameterIn.HEADER,
                    description = "Document identifier required for auditing purposes.",
                    example = "100-352",
                    required = true,
                    schema = @Schema(type = "string", example = "100-352")
            ),
            @Parameter(
                    name = "X-Action-Requested",
                    in = ParameterIn.HEADER,
                    description = "Action requested must match this endpoint's @AllowedAction (EDIT).",
                    example = "EDIT",
                    required = true,
                    schema = @Schema(type = "string", example = "EDIT")
            )
    })
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully updated Export BL record and details",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ExportManifestUpdateResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or validation error",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Export BL record not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> updateExportBl(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated Export BL and details", required = true)
            @Valid @RequestBody ExportManifestUpdateRequest request) {
        return success("Export BL and details updated successfully", service.updateExportBlCombined(id, request));
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @Operation(summary = "Delete Export BL", description = "Soft deletes an Export BL manifest")
    @Parameters({
            @Parameter(
                    name = "X-Document-Id",
                    in = ParameterIn.HEADER,
                    description = "Document identifier required for auditing purposes.",
                    example = "100-352",
                    required = true,
                    schema = @Schema(type = "string", example = "100-352")
            ),
            @Parameter(
                    name = "X-Action-Requested",
                    in = ParameterIn.HEADER,
                    description = "Action requested must match this endpoint's @AllowedAction (DELETE).",
                    example = "DELETE",
                    required = true,
                    schema = @Schema(type = "string", example = "DELETE")
            )
    })
    @DeleteMapping("/{transactionPoid}")
    public ResponseEntity<?> deleteExportBl(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
        service.deleteExportBl(transactionPoid);
        return success("Export BL deleted successfully");
    }

    // ========== General Cargo Details Operations ==========



    // ========== Container Details Operations ==========

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Get Cargo and Container Details", description = "Retrieves container details and cargo description for an Export BL")
    @Parameters({
            @Parameter(
                    name = "X-Document-Id",
                    in = ParameterIn.HEADER,
                    description = "Document identifier required for auditing purposes.",
                    example = "100-352",
                    required = true,
                    schema = @Schema(type = "string", example = "100-352")
            ),
            @Parameter(
                    name = "X-Action-Requested",
                    in = ParameterIn.HEADER,
                    description = "Action requested must match this endpoint's @AllowedAction (VIEW).",
                    example = "VIEW",
                    required = true,
                    schema = @Schema(type = "string", example = "VIEW")
            )
    })
    @GetMapping("/{transactionPoid}/cargo-container-details")
    public ResponseEntity<?> getCargoContainerDetails(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
        return success("Cargo and container details retrieved successfully", service.getCargoContainerDetails(transactionPoid));
    }



    // ========== Cargo Description and Marks Operations ==========



    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Get Cargo Marks", description = "Retrieves all cargo marks details for an Export BL")
    @Parameters({
            @Parameter(
                    name = "X-Document-Id",
                    in = ParameterIn.HEADER,
                    description = "Document identifier required for auditing purposes.",
                    example = "100-352",
                    required = true,
                    schema = @Schema(type = "string", example = "100-352")
            ),
            @Parameter(
                    name = "X-Action-Requested",
                    in = ParameterIn.HEADER,
                    description = "Action requested must match this endpoint's @AllowedAction (VIEW).",
                    example = "VIEW",
                    required = true,
                    schema = @Schema(type = "string", example = "VIEW")
            )
    })
    @GetMapping("/{transactionPoid}/cargo-marks")
    public ResponseEntity<?> getCargoMarks(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
        return success("Cargo marks retrieved successfully", service.getCargoMarks(transactionPoid));
    }



    // ========== Charge Details Operations ==========

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Get Charge Details", description = "Retrieves all charge details for an Export BL")
    @Parameters({
            @Parameter(
                    name = "X-Document-Id",
                    in = ParameterIn.HEADER,
                    description = "Document identifier required for auditing purposes.",
                    example = "100-352",
                    required = true,
                    schema = @Schema(type = "string", example = "100-352")
            ),
            @Parameter(
                    name = "X-Action-Requested",
                    in = ParameterIn.HEADER,
                    description = "Action requested must match this endpoint's @AllowedAction (VIEW).",
                    example = "VIEW",
                    required = true,
                    schema = @Schema(type = "string", example = "VIEW")
            )
    })
    @GetMapping("/{transactionPoid}/charge-details")
    public ResponseEntity<?> getChargeDetails(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
        return success("Charge details retrieved successfully", service.getChargeDetails(transactionPoid));
    }



    // ========== Special Operations ==========

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(summary = "Load Booking Data", description = "Loads booking data from MATE (pending mate bookings) into the Export BL")
    @Parameters({
            @Parameter(
                    name = "X-Document-Id",
                    in = ParameterIn.HEADER,
                    description = "Document identifier required for auditing purposes.",
                    example = "100-352",
                    required = true,
                    schema = @Schema(type = "string", example = "100-352")
            ),
            @Parameter(
                    name = "X-Action-Requested",
                    in = ParameterIn.HEADER,
                    description = "Action requested must match this endpoint's @AllowedAction (EDIT).",
                    example = "EDIT",
                    required = true,
                    schema = @Schema(type = "string", example = "EDIT")
            )
    })
    @PostMapping("/{transactionPoid}/load-booking")
    public ResponseEntity<?> loadBooking(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Load booking request", required = true)
            @Valid @RequestBody LoadBookingRequest request) {
        return success("Booking data loaded successfully", service.loadBooking(transactionPoid, request));
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Generate BL Print", description = "Generates BL print (original or draft)")
    @Parameters({
            @Parameter(
                    name = "X-Document-Id",
                    in = ParameterIn.HEADER,
                    description = "Document identifier required for auditing purposes.",
                    example = "100-352",
                    required = true,
                    schema = @Schema(type = "string", example = "100-352")
            ),
            @Parameter(
                    name = "X-Action-Requested",
                    in = ParameterIn.HEADER,
                    description = "Action requested must match this endpoint's @AllowedAction (VIEW).",
                    example = "VIEW",
                    required = true,
                    schema = @Schema(type = "string", example = "VIEW")
            )
    })
    @PostMapping("/{transactionPoid}/generate-bl-print")
    public ResponseEntity<?> generateBlPrint(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Generate BL print request", required = true)
            @Valid @RequestBody GenerateBlPrintRequest request) {
    	 try {
    		 String docId=UserContext.getDocumentId();
             byte[] pdf = service.generateBlPrint(transactionPoid, request,docId);
             return ResponseEntity.ok()
                     .header(HttpHeaders.CONTENT_DISPOSITION,
                             "attachment; filename=bl-print-" + transactionPoid + ".pdf")
                     .contentType(MediaType.APPLICATION_PDF)
                     .body(pdf);
         } catch (Exception e) {
             log.error("Failed to generate PDF for BL Print: {}", transactionPoid, e);
             return error("Failed to generate PDF: " + e.getMessage(), 500);
         }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Generate Manifest", description = "Generates cargo manifest or freight manifest")
    @Parameters({
            @Parameter(
                    name = "X-Document-Id",
                    in = ParameterIn.HEADER,
                    description = "Document identifier required for auditing purposes.",
                    example = "100-352",
                    required = true,
                    schema = @Schema(type = "string", example = "100-352")
            ),
            @Parameter(
                    name = "X-Action-Requested",
                    in = ParameterIn.HEADER,
                    description = "Action requested must match this endpoint's @AllowedAction (VIEW).",
                    example = "VIEW",
                    required = true,
                    schema = @Schema(type = "string", example = "VIEW")
            )
    })
    @PostMapping("/{transactionPoid}/generate-manifest")
    public ResponseEntity<?> generateManifest(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Generate manifest request", required = true)
            @Valid @RequestBody GenerateManifestRequest request) {
        try {
        	String docId=UserContext.getDocumentId();
			byte[] pdf = service.generateManifest(transactionPoid, request, docId);
			String fileName = request.getFreightCargo().toString().equalsIgnoreCase("FALSE") ? "cargo-manifest-" : "freight-manifest-";
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION,
							"attachment; filename=" + fileName + transactionPoid + ".pdf")
					.contentType(MediaType.APPLICATION_PDF).body(pdf);
		} catch (Exception e) {
			log.error("Failed to generate PDF for Day Close Shipping: {}", transactionPoid, e);
			return error("Failed to generate PDF: " + e.getMessage(), 500);
		}
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Generate Detention/Storage Report", description = "Generates detention/storage report")
    @Parameters({
            @Parameter(
                    name = "X-Document-Id",
                    in = ParameterIn.HEADER,
                    description = "Document identifier required for auditing purposes.",
                    example = "100-352",
                    required = true,
                    schema = @Schema(type = "string", example = "100-352")
            ),
            @Parameter(
                    name = "X-Action-Requested",
                    in = ParameterIn.HEADER,
                    description = "Action requested must match this endpoint's @AllowedAction (VIEW).",
                    example = "VIEW",
                    required = true,
                    schema = @Schema(type = "string", example = "VIEW")
            )
    })
    @PostMapping("/{transactionPoid}/generate-detention-storage")
    public ResponseEntity<?> generateDetentionStorage(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
    	try {
   		 String docId=UserContext.getDocumentId();
            byte[] pdf = service.generateDetentionStorage(transactionPoid,docId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=detention-storage-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate PDF for detention or port storage: {}", transactionPoid, e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(summary = "Export EDI", description = "Exports BL data to EDI format")
    @Parameters({
            @Parameter(
                    name = "X-Document-Id",
                    in = ParameterIn.HEADER,
                    description = "Document identifier required for auditing purposes.",
                    example = "100-352",
                    required = true,
                    schema = @Schema(type = "string", example = "100-352")
            ),
            @Parameter(
                    name = "X-Action-Requested",
                    in = ParameterIn.HEADER,
                    description = "Action requested must match this endpoint's @AllowedAction (EDIT).",
                    example = "EDIT",
                    required = true,
                    schema = @Schema(type = "string", example = "EDIT")
            )
    })
    @PostMapping("/{transactionPoid}/export-edi")
    public ResponseEntity<?> exportEdi(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
        service.exportEdi(transactionPoid);
        return success("EDI exported successfully");
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Validate Export BL", description = "Validates Export BL before save")
    @Parameters({
            @Parameter(
                    name = "X-Document-Id",
                    in = ParameterIn.HEADER,
                    description = "Document identifier required for auditing purposes.",
                    example = "100-352",
                    required = true,
                    schema = @Schema(type = "string", example = "100-352")
            ),
            @Parameter(
                    name = "X-Action-Requested",
                    in = ParameterIn.HEADER,
                    description = "Action requested must match this endpoint's @AllowedAction (VIEW).",
                    example = "VIEW",
                    required = true,
                    schema = @Schema(type = "string", example = "VIEW")
            )
    })
    @PostMapping("/{transactionPoid}/validate")
    public ResponseEntity<?> validate(
            @Parameter(description = "Transaction POID (optional for new records)", required = false) 
            @PathVariable(required = false) Long transactionPoid,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Export BL request to validate", required = true)
            @Valid @RequestBody ExportManifestBlRequest request) {
        return success("Validation completed", service.validate(transactionPoid, request));
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(summary = "Post-Save Processing", description = "Post-save processing (called after successful save)")
    @Parameters({
            @Parameter(
                    name = "X-Document-Id",
                    in = ParameterIn.HEADER,
                    description = "Document identifier required for auditing purposes.",
                    example = "100-352",
                    required = true,
                    schema = @Schema(type = "string", example = "100-352")
            ),
            @Parameter(
                    name = "X-Action-Requested",
                    in = ParameterIn.HEADER,
                    description = "Action requested must match this endpoint's @AllowedAction (EDIT).",
                    example = "EDIT",
                    required = true,
                    schema = @Schema(type = "string", example = "EDIT")
            )
    })
    @PostMapping("/{transactionPoid}/after-save")
    public ResponseEntity<?> afterSave(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
        service.afterSave(transactionPoid);
        return success("Post-save processing completed");
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Get BL Status", description = "Gets BL status information")
    @Parameters({
            @Parameter(
                    name = "X-Document-Id",
                    in = ParameterIn.HEADER,
                    description = "Document identifier required for auditing purposes.",
                    example = "100-352",
                    required = true,
                    schema = @Schema(type = "string", example = "100-352")
            ),
            @Parameter(
                    name = "X-Action-Requested",
                    in = ParameterIn.HEADER,
                    description = "Action requested must match this endpoint's @AllowedAction (VIEW).",
                    example = "VIEW",
                    required = true,
                    schema = @Schema(type = "string", example = "VIEW")
            )
    })
    @GetMapping("/{transactionPoid}/bl-status")
    public ResponseEntity<?> getBlStatus(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
        return success("BL status retrieved successfully", service.getBlStatus(transactionPoid));
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Quotation After Browse", description = "Processes after quotation LOV browse (auto-populates fields from quotation)")
    @Parameters({
            @Parameter(
                    name = "X-Document-Id",
                    in = ParameterIn.HEADER,
                    description = "Document identifier required for auditing purposes.",
                    example = "100-352",
                    required = true,
                    schema = @Schema(type = "string", example = "100-352")
            ),
            @Parameter(
                    name = "X-Action-Requested",
                    in = ParameterIn.HEADER,
                    description = "Action requested must match this endpoint's @AllowedAction (VIEW).",
                    example = "VIEW",
                    required = true,
                    schema = @Schema(type = "string", example = "VIEW")
            )
    })
    @PostMapping("/{transactionPoid}/quotation-after-browse")
    public ResponseEntity<?> quotationAfterBrowse(
            @Parameter(description = "Transaction POID (optional for new records)", required = false)
            @PathVariable(required = false) Long transactionPoid,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Quotation after browse request", required = true)
            @Valid @RequestBody QuotationAfterBrowseRequest request) {
        return success("Quotation data loaded successfully", 
                service.quotationAfterBrowse(transactionPoid, request));
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Get Address Details", description = "Retrieves address details for a customer by address master POID and address type")
    @Parameters({
            @Parameter(
                    name = "X-Document-Id",
                    in = ParameterIn.HEADER,
                    description = "Document identifier required for auditing purposes.",
                    example = "100-352",
                    required = true,
                    schema = @Schema(type = "string", example = "100-352")
            ),
            @Parameter(
                    name = "X-Action-Requested",
                    in = ParameterIn.HEADER,
                    description = "Action requested must match this endpoint's @AllowedAction (VIEW).",
                    example = "VIEW",
                    required = true,
                    schema = @Schema(type = "string", example = "VIEW")
            )
    })
    @GetMapping("/address-details")
    public ResponseEntity<?> getAddressDetails(
            @Parameter(description = "Address Master POID", required = true) @RequestParam Long addressMasterPoid,
            @Parameter(description = "Address Type", required = false) @RequestParam(required = false, defaultValue = "CAN") String addressType) {
        return success("Address details retrieved successfully", service.getAddressDetails(addressMasterPoid, addressType));
    }
}

