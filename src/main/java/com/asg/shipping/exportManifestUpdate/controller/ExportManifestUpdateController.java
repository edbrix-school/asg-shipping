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
    @GetMapping("/{transactionPoid}")
    public ResponseEntity<?> getExportBlById(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
        try {
            return success("Export BL retrieved successfully", service.getExportBlById(transactionPoid));
        } catch (Exception e) {
            log.error("Error retrieving Export BL with ID: {}", transactionPoid, e);
            return error("Error retrieving Export BL: " + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Search Export BLs", description = "Searches and retrieves a paginated list of Export BL manifests")
    @PostMapping("/list")
    public ResponseEntity<?> searchExportBls(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters) {
        try {
            return success("Export BL list fetched successfully", service.searchExportBls(filters, pageable));
        } catch (Exception e) {
            log.error("Error searching Export BLs", e);
            return error("Error fetching Export BL list: " + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @Operation(summary = "Create Export BL", description = "Creates a new Export BL manifest header")
    @PostMapping
    public ResponseEntity<?> createExportBl(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Export BL details", required = true)
            @Valid @RequestBody ExportManifestBlRequest request) {
        try {
            return success("Export BL created successfully", service.createExportBl(request));
        } catch (Exception e) {
            log.error("Error creating Export BL", e);
            return error("Error creating Export BL: " + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{id}")
    @Operation(
            summary = "Update Export BL and Details",
            description = "Updates an existing Export BL manifest header and related details (general cargo, containers, cargo description, marks, charges)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully updated Export BL record and details",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ExportManifestUpdateResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request data or validation error",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Export BL record not found",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> updateExportBl(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated Export BL and details", required = true)
            @Valid @RequestBody ExportManifestUpdateRequest request) {
        try {
            return success("Export BL and details updated successfully", service.updateExportBlCombined(id, request));
        } catch (Exception e) {
            log.error("Error updating Export BL with ID: {}", id, e);
            return error("Error updating Export BL: " + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @Operation(summary = "Delete Export BL", description = "Soft deletes an Export BL manifest")
    @DeleteMapping("/{transactionPoid}")
    public ResponseEntity<?> deleteExportBl(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
        try {
            service.deleteExportBl(transactionPoid);
            return success("Export BL deleted successfully");
        } catch (Exception e) {
            log.error("Error deleting Export BL with ID: {}", transactionPoid, e);
            return error("Error deleting Export BL: " + e.getMessage(), 500);
        }
    }

    // ========== Cargo / Container Details ==========

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Get Cargo and Container Details", description = "Retrieves container details and cargo description/marks for an Export BL")
    @GetMapping("/{transactionPoid}/cargo-container-details")
    public ResponseEntity<?> getCargoContainerDetails(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
        try {
            return success("Cargo and container details retrieved successfully", service.getCargoContainerDetails(transactionPoid));
        } catch (Exception e) {
            log.error("Error retrieving cargo/container details for ID: {}", transactionPoid, e);
            return error("Error retrieving cargo/container details: " + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Get Cargo Marks", description = "Retrieves all cargo marks details for an Export BL")
    @GetMapping("/{transactionPoid}/cargo-marks")
    public ResponseEntity<?> getCargoMarks(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
        try {
            return success("Cargo marks retrieved successfully", service.getCargoMarks(transactionPoid));
        } catch (Exception e) {
            log.error("Error retrieving cargo marks for ID: {}", transactionPoid, e);
            return error("Error retrieving cargo marks: " + e.getMessage(), 500);
        }
    }

    // ========== Charge Details ==========

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Get Charge Details", description = "Retrieves all charge details for an Export BL")
    @GetMapping("/{transactionPoid}/charge-details")
    public ResponseEntity<?> getChargeDetails(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
        try {
            return success("Charge details retrieved successfully", service.getChargeDetails(transactionPoid));
        } catch (Exception e) {
            log.error("Error retrieving charge details for ID: {}", transactionPoid, e);
            return error("Error retrieving charge details: " + e.getMessage(), 500);
        }
    }

    // ========== Special Operations ==========

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(summary = "Load Booking Data", description = "Loads booking data from MATE (pending mate bookings) into the Export BL")
    @PostMapping("/{transactionPoid}/load-booking")
    public ResponseEntity<?> loadBooking(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Load booking request", required = true)
            @Valid @RequestBody LoadBookingRequest request) {
        try {
            return success("Booking data loaded successfully", service.loadBooking(transactionPoid, request));
        } catch (UnsupportedOperationException e) {
            log.warn("Load booking not yet implemented for ID: {}", transactionPoid);
            return error("Load booking is not yet implemented", 501);
        } catch (Exception e) {
            log.error("Error loading booking for ID: {}", transactionPoid, e);
            return error("Error loading booking: " + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Generate BL Print", description = "Generates BL print (original or draft)")
    @PostMapping("/{transactionPoid}/generate-bl-print")
    public ResponseEntity<?> generateBlPrint(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Generate BL print request", required = true)
            @Valid @RequestBody GenerateBlPrintRequest request) {
        try {
            String docId = UserContext.getDocumentId();
            byte[] pdf = service.generateBlPrint(transactionPoid, request, docId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=bl-print-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate BL Print for ID: {}", transactionPoid, e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Generate Manifest", description = "Generates cargo manifest or freight manifest")
    @PostMapping("/{transactionPoid}/generate-manifest")
    public ResponseEntity<?> generateManifest(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Generate manifest request", required = true)
            @Valid @RequestBody GenerateManifestRequest request) {
        try {
            String docId = UserContext.getDocumentId();
            byte[] pdf = service.generateManifest(transactionPoid, request, docId);
            String fileName = request.getFreightCargo().toString().equalsIgnoreCase("FALSE")
                    ? "cargo-manifest-" : "freight-manifest-";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=" + fileName + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate manifest for ID: {}", transactionPoid, e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Generate Detention/Storage Report", description = "Generates detention/storage report")
    @PostMapping("/{transactionPoid}/generate-detention-storage")
    public ResponseEntity<?> generateDetentionStorage(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
        try {
            String docId = UserContext.getDocumentId();
            byte[] pdf = service.generateDetentionStorage(transactionPoid, docId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=detention-storage-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate detention/storage report for ID: {}", transactionPoid, e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(summary = "Export EDI", description = "Exports BL data to EDI format")
    @PostMapping("/{transactionPoid}/export-edi")
    public ResponseEntity<?> exportEdi(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
        try {
            service.exportEdi(transactionPoid);
            return success("EDI exported successfully");
        } catch (Exception e) {
            log.error("Error exporting EDI for ID: {}", transactionPoid, e);
            return error("Error exporting EDI: " + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Validate Export BL",
            description = "Validates Export BL before save. Pass transactionPoid as query param for existing records, omit for new records.")
    @PostMapping("/validate")
    public ResponseEntity<?> validate(
            @Parameter(description = "Transaction POID (optional — omit for new records)")
            @RequestParam(required = false) Long transactionPoid,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Export BL request to validate", required = true)
            @Valid @RequestBody ExportManifestBlRequest request) {
        try {
            return success("Validation completed", service.validate(transactionPoid, request));
        } catch (Exception e) {
            log.error("Error validating Export BL", e);
            return error("Error validating Export BL: " + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(summary = "Post-Save Processing", description = "Post-save processing (called after successful save)")
    @PostMapping("/{transactionPoid}/after-save")
    public ResponseEntity<?> afterSave(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
        try {
            service.afterSave(transactionPoid);
            return success("Post-save processing completed");
        } catch (Exception e) {
            log.error("Error in after-save processing for ID: {}", transactionPoid, e);
            return error("Error in post-save processing: " + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Get BL Status", description = "Gets BL status information")
    @GetMapping("/{transactionPoid}/bl-status")
    public ResponseEntity<?> getBlStatus(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
        try {
            return success("BL status retrieved successfully", service.getBlStatus(transactionPoid));
        } catch (Exception e) {
            log.error("Error retrieving BL status for ID: {}", transactionPoid, e);
            return error("Error retrieving BL status: " + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Quotation After Browse",
            description = "Processes after quotation LOV browse. Pass transactionPoid as query param for existing records, omit for new records.")
    @PostMapping("/quotation-after-browse")
    public ResponseEntity<?> quotationAfterBrowse(
            @Parameter(description = "Transaction POID (optional — omit for new records)")
            @RequestParam(required = false) Long transactionPoid,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Quotation after browse request", required = true)
            @Valid @RequestBody QuotationAfterBrowseRequest request) {
        try {
            return success("Quotation data loaded successfully",
                    service.quotationAfterBrowse(transactionPoid, request));
        } catch (Exception e) {
            log.error("Error processing quotation after browse", e);
            return error("Error processing quotation after browse: " + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Get Address Details", description = "Retrieves address details for a customer by address master POID and address type")
    @GetMapping("/address-details")
    public ResponseEntity<?> getAddressDetails(
            @Parameter(description = "Address Master POID", required = true) @RequestParam Long addressMasterPoid,
            @Parameter(description = "Address Type") @RequestParam(required = false, defaultValue = "CAN") String addressType) {
        try {
            return success("Address details retrieved successfully", service.getAddressDetails(addressMasterPoid, addressType));
        } catch (Exception e) {
            log.error("Error retrieving address details for master POID: {}", addressMasterPoid, e);
            return error("Error retrieving address details: " + e.getMessage(), 500);
        }
    }
}
