package com.asg.shipping.exportManifestUpdate.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.exportManifestUpdate.dto.*;
import com.asg.shipping.exportManifestUpdate.service.ExportManifestBlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
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
        private final LoggingService loggingService;

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
        log.info("VIEW: Getting Export BL with transactionPoid: {}", transactionPoid);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
        ExportManifestBlResponse response = service.getExportBlById(transactionPoid);
        log.debug("VIEW: Successfully retrieved Export BL. BL Number: {}, Status: {}", 
                response.getBlNumber(), response.getBlStatus());
        return success("Export BL retrieved successfully", response);
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
        log.info("VIEW: Searching Export BLs with page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        var result = service.searchExportBls(filters, pageable);
        log.debug("VIEW: Search completed successfully");
        return success("Export BL list fetched successfully", result);
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
    @Operation(summary = "Update Export BL", description = "Updates an existing Export BL manifest header")
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
    @PutMapping("/{transactionPoid}")
    public ResponseEntity<?> updateExportBl(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated Export BL details", required = true)
            @Valid @RequestBody ExportManifestBlRequest request) {
        return success("Export BL updated successfully", service.updateExportBl(transactionPoid, request));
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

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Get General Cargo Details", description = "Retrieves all general cargo details for an Export BL")
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
    @GetMapping("/{transactionPoid}/general-cargo-details")
    public ResponseEntity<?> getGeneralCargoDetails(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
        log.info("VIEW: Getting general cargo details for transactionPoid: {}", transactionPoid);
                loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
        var details = service.getGeneralCargoDetails(transactionPoid);
        log.debug("VIEW: Retrieved {} general cargo details", details != null ? details.size() : 0);
        return success("General cargo details retrieved successfully", details);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(summary = "Bulk Save General Cargo Details", description = "Bulk save general cargo details (create, update, delete in single transaction)")
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
    @PostMapping("/{transactionPoid}/general-cargo-details/bulk-save")
    public ResponseEntity<?> bulkSaveGeneralCargoDetails(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Bulk save request", required = true)
            @Valid @RequestBody BulkSaveRequest<GeneralCargoDetailDto> request) {
        return success("General cargo details saved successfully", 
                service.bulkSaveGeneralCargoDetails(transactionPoid, request));
    }

    // ========== Container Details Operations ==========

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Get Container Details", description = "Retrieves all container details for an Export BL")
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
    @GetMapping("/{transactionPoid}/container-details")
    public ResponseEntity<?> getContainerDetails(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
        log.info("VIEW: Getting container details for transactionPoid: {}", transactionPoid);
                loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
        var details = service.getContainerDetails(transactionPoid);
        log.debug("VIEW: Retrieved {} container details", details != null ? details.size() : 0);
        return success("Container details retrieved successfully", details);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(summary = "Bulk Save Container Details", description = "Bulk save container details (create, update, delete in single transaction)")
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
    @PostMapping("/{transactionPoid}/container-details/bulk-save")
    public ResponseEntity<?> bulkSaveContainerDetails(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Bulk save request", required = true)
            @Valid @RequestBody BulkSaveRequest<ContainerDetailDto> request) {
        return success("Container details saved successfully", 
                service.bulkSaveContainerDetails(transactionPoid, request));
    }

    // ========== Cargo Description and Marks Operations ==========

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Get Cargo Description", description = "Retrieves all cargo description details for an Export BL")
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
    @GetMapping("/{transactionPoid}/cargo-description")
    public ResponseEntity<?> getCargoDescription(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
        log.info("VIEW: Getting cargo description for transactionPoid: {}", transactionPoid);
                loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
        var descriptions = service.getCargoDescription(transactionPoid);
        log.debug("VIEW: Retrieved {} cargo descriptions", descriptions != null ? descriptions.size() : 0);
        return success("Cargo description retrieved successfully", descriptions);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(summary = "Bulk Save Cargo Description", description = "Bulk save cargo description (create, update, delete in single transaction)")
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
    @PostMapping("/{transactionPoid}/cargo-description/bulk-save")
    public ResponseEntity<?> bulkSaveCargoDescription(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Bulk save request", required = true)
            @Valid @RequestBody BulkSaveRequest<CargoDescriptionDto> request) {
        return success("Cargo description saved successfully", 
                service.bulkSaveCargoDescription(transactionPoid, request));
    }

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
        log.info("VIEW: Getting cargo marks for transactionPoid: {}", transactionPoid);
                loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
        var marks = service.getCargoMarks(transactionPoid);
        log.debug("VIEW: Retrieved {} cargo marks", marks != null ? marks.size() : 0);
        return success("Cargo marks retrieved successfully", marks);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(summary = "Bulk Save Cargo Marks", description = "Bulk save cargo marks (create, update, delete in single transaction)")
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
    @PostMapping("/{transactionPoid}/cargo-marks/bulk-save")
    public ResponseEntity<?> bulkSaveCargoMarks(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Bulk save request", required = true)
            @Valid @RequestBody BulkSaveRequest<CargoMarksDto> request) {
        return success("Cargo marks saved successfully", 
                service.bulkSaveCargoMarks(transactionPoid, request));
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
        log.info("VIEW: Getting charge details for transactionPoid: {}", transactionPoid);
                loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
        var chargeData = service.getChargeDetails(transactionPoid);
        @SuppressWarnings("unchecked")
        java.util.List<ChargeDetailDto> details = (java.util.List<ChargeDetailDto>) chargeData.get("data");
        log.debug("VIEW: Retrieved {} charge details", details != null ? details.size() : 0);
        return success("Charge details retrieved successfully", chargeData);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(summary = "Bulk Save Charge Details", description = "Bulk save charge details (create, update, delete in single transaction)")
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
    @PostMapping("/{transactionPoid}/charge-details/bulk-save")
    public ResponseEntity<?> bulkSaveChargeDetails(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Bulk save request", required = true)
            @Valid @RequestBody BulkSaveRequest<ChargeDetailDto> request) {
        return success("Charge details saved successfully", 
                service.bulkSaveChargeDetails(transactionPoid, request));
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

    @AllowedAction(UserRolesRightsEnum.PRINT)
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
                log.info("PRINT: Generating BL print for transactionPoid: {}", transactionPoid);
                loggingService.createLogSummaryEntry(LogDetailsEnum.PREVIEWED_OR_PRINTED_OR_DOWNLOADED, UserContext.getDocumentId(), transactionPoid.toString());
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

    @AllowedAction(UserRolesRightsEnum.PRINT)
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
                log.info("PRINT: Generating Manifest for transactionPoid: {}", transactionPoid);
                loggingService.createLogSummaryEntry(LogDetailsEnum.PREVIEWED_OR_PRINTED_OR_DOWNLOADED, UserContext.getDocumentId(), transactionPoid.toString());
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

    @AllowedAction(UserRolesRightsEnum.PRINT)
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
                log.info("PRINT: Generating Detention Storage for transactionPoid: {}", transactionPoid);
                loggingService.createLogSummaryEntry(LogDetailsEnum.PREVIEWED_OR_PRINTED_OR_DOWNLOADED, UserContext.getDocumentId(), transactionPoid.toString());
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
                loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
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
}

