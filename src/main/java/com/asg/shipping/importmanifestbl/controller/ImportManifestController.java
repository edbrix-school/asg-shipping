package com.asg.shipping.importmanifestbl.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.importManifestUpdate.dto.*;
import com.asg.shipping.importmanifestbl.dto.*;
import com.asg.shipping.importmanifestbl.dto.LoadEmailFaxRequestDto;
import com.asg.shipping.importmanifestbl.service.ImportManifestService;
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

import java.time.LocalDate;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;
import static com.asg.common.lib.security.util.UserContext.getCompanyPoid;
import static com.asg.common.lib.security.util.UserContext.getGroupPoid;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("v1/import-manifest-bl")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "import-manifest-bl-controller", description = "Manage Import Manifest BL records")
public class ImportManifestController {

    private final ImportManifestService importManifestService;

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @Operation(
            summary = "Create Import Manifest BL",
            description = "Create a new Import Manifest BL record with details."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Import Manifest BL created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody ImportManifestBlCreateDto request
    ) {
        try {
            Long companyPoid = com.asg.common.lib.security.util.UserContext.getCompanyPoid();
            Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();
            ImportManifestBlRequestDto response = importManifestService.createImportManifestBl(request, companyPoid, groupPoid);
            return success("Import Manifest BL created successfully", response);
        } catch (ValidationException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to create Import Manifest BL: " + e.getMessage());
        }
    }


    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Import Manifest BL retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Import Manifest BL not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getImportManifest(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long id
    ) {
        try {
            ImportManifestBlRequestDto response = importManifestService.getImportManifest(id);
            return success("Import Manifest BL retrieved successfully", response);
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to retrieve Import Manifest BL: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @Operation(
            summary = "Delete Import Manifest BL",
            description = "Delete an Import Manifest BL record by transaction POID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Import Manifest BL deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Import Manifest BL not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{transactionPoId}")
    public ResponseEntity<?> delete(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoId
    ) {
        try {
            importManifestService.delete(transactionPoId);
            return success("Import Manifest BL deleted successfully", null);
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to delete Import Manifest BL: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{id}")
    @Operation(
            summary = "Update Import Manifest BL record",
            description = "Update an existing Import Manifest BL record with validation",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully updated Import Manifest BL record",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ImportManifestBlRequestDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or validation error",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Import Manifest BL record not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> updateImportManifestBl(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long id,
            @Valid @RequestBody ImportManifestBlUpdateDTO dto) {

        Long companyPoid = getCompanyPoid();
        Long groupPoid = getGroupPoid();

        ImportManifestBlRequestDto updated = importManifestService.updateImportManifestBl(id, dto, companyPoid, groupPoid);

        return success("Import Manifest BL updated successfully", updated);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(
            summary = "Update Email Verification",
            description = "Update email verification status for Import Manifest BL."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email verification updated successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Import Manifest BL not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/update-email-verification")
    public ResponseEntity<?> updateEmailVerification(
            @Valid @RequestBody EmailVerificationRequestDto request
    ) {
        try {
            EmailVerificationResponseDto response = importManifestService.updateEmailVerification(request.getTransactionPoId(), request);
            return success("Email verification updated successfully", response);
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to update email verification: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(
            summary = "Resend CAN",
            description = "Resend Cargo Arrival Notice for Import Manifest BL."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "CAN resent successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Import Manifest BL not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/resend-can")
    public ResponseEntity<?> resendCan(
            @Valid @RequestBody ResendCanRequestDto request
    ) {
        try {
            ResendCanResponseDto response = importManifestService.resendCan(request.getTransactionPoId());
            return success("CAN resent successfully", response);
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to resend CAN: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(
            summary = "Send EDI Emails",
            description = "Send EDI emails for Import Manifest BL."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "EDI emails sent successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Import Manifest BL not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error@Put")
    })
    @PostMapping("/send-edi-emails")
    public ResponseEntity<?> sendEdiEmails(
            @Valid @RequestBody SendEdiEmailsRequestDto request
    ) {
        try {
            SendEdiEmailsResponseDto response = importManifestService.sendEdiEmails(request.getTransactionPoId());
            return success("EDI emails sent successfully", response);
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to send EDI emails: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(
            summary = "Load Email/Fax Data",
            description = "Load email/fax data for selected party."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email/Fax data loaded successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Import Manifest BL not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/load-email-fax")
    public ResponseEntity<?> loadEmailFax(
            @Valid @RequestBody LoadEmailFaxRequestDto request
    ) {
        try {
            LoadEmailFaxResponseDto response = importManifestService.loadEmailFax(request.getTransactionPoId(), null);
            return success("Email/Fax data loaded successfully", response);
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to load email/fax data: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "Get BL Status",
            description = "Get BL status information for DO."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "BL status retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Import Manifest BL not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}/bl-status")
    public ResponseEntity<?> getBlStatus(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long id
    ) {
        try {
            BlStatusResponseDto response = importManifestService.getBlStatus(id);
            return success("BL status retrieved successfully", response);
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to retrieve BL status: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "List Import Manifest BLs",
            description = """
                    Fetch Import Manifest BL records using filters and pagination.
                    
                    Valid `searchField` values: BL_NUMBER, TRANSACTION_POID, VOYAGE_NUMBER, VESSEL_NAME, CONSIGNEE_NAME, SHIPPER_NAME
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    examples = @ExampleObject(
                            name = "Import Manifest BL Filters",
                            value = """
                                    {
                                      "operator": "AND",
                                      "isDeleted": "N",
                                      "filters": [
                                        {
                                          "searchField": "BL_NUMBER",
                                          "searchValue": "BL123456"
                                        },
                                        {
                                          "searchField": "VESSEL_NAME",
                                          "searchValue": "VESSEL001"
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Import Manifest BL list retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/list")
    public ResponseEntity<?> list(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters
    ) {
        try {
            Map<String, Object> response = importManifestService.list(filters, pageable);
            return success("Import Manifest BL list retrieved successfully", response);
        } catch (Exception e) {
            return internalServerError("Failed to retrieve Import Manifest BL list: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "Get Default Values",
            description = "Fetch default values for Import Manifest BL creation based on document ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Default values retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/default-values")
    public ResponseEntity<?> getDefaultValues(
    ){
        try {
            DefaultValueDto response = importManifestService.getDefaultValues(UserContext.getDocumentId());
            if (response == null) {
                return notFound("No default values found for docId: " + UserContext.getDocumentId());
            }
            return success("Default values retrieved successfully", response);
        } catch (Exception e) {
            return internalServerError("Failed to retrieve default values: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "Get Container Types and Commodities Dropdown",
            description = "Fetch container types and commodities for a specific voyage to populate dropdown lists."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Container types and commodities retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/containers-dropdown")
    public ResponseEntity<?> getContainerTypesByVoyage(
            @Parameter(description = "Voyage Transaction POID", example = "12345")
            @RequestParam(required = false) Long voyageTransPoid) {

        ContainersDropDownDto containerTypes = importManifestService.getContainerTypesByVoyage(voyageTransPoid);
        return ResponseEntity.ok(containerTypes);
    }


    @GetMapping("/uncleared-cargo-notice/{transactionPoid}")
    public ResponseEntity<?> printUnclearedCargoNotice(
            @Parameter(description = "Transaction POID", example = "12345")
            @PathVariable Long transactionPoid
          ) {
        try {
            byte[] pdf = importManifestService.printUnclearedCargoNotice(transactionPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=import-manifest-bl-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {

            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }

    }

    @GetMapping("/proforma-invoice/{transactionPoid}")
    public ResponseEntity<?> printProformaInvoice(
            @Parameter(description = "Transaction POID", example = "12345")
            @PathVariable Long transactionPoid,
            @Parameter(description = "Demurrage Charges", example = "100.00")
            @RequestParam(required = false) LocalDate demChargesTill,
            @Parameter(description = "Percentage", example = "5.0")
            @RequestParam(required = false) Long percentage
    ) {
        try {
            byte[] pdf = importManifestService.printProformaInvoice(transactionPoid,demChargesTill,percentage);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=import-manifest-bl-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }

    }

    @GetMapping("/cargo-arrival-notice/{transactionPoid}")
    public ResponseEntity<?> printCargoArrivalNotice(
            @Parameter(description = "Transaction POID", example = "12345")
            @PathVariable Long transactionPoid,
            @Parameter(description = "Voyage Transaction POID", example = "67890", required = true)
            @RequestParam Long voyageTransactionPoid
    ) {
        try {
            byte[] pdf = importManifestService.printCargoArrivalNotice(voyageTransactionPoid,transactionPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=import-manifest-bl-cargo-arrival-notice-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("error",e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }

    @GetMapping("/cargo-manifest-print/{transactionPoid}")
    public ResponseEntity<?> printCargoManifest(
            @Parameter(description = "Transaction POID", example = "12345")
            @PathVariable Long transactionPoid,
            @Parameter(description = "Is Cargo Manifest Print", example = "true")
            @RequestParam(defaultValue = "false") boolean isCargoManifestPrint
    ) {
        try {
            byte[] pdf = importManifestService.printCargoManifest(transactionPoid, isCargoManifestPrint);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=import-manifest-bl-cargo-manifest-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("error",e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }

    @GetMapping("/port-charges/{transactionPoid}")
    public ResponseEntity<?> printCheckPortCharges(
            @Parameter(description = "Transaction POID", example = "12345")
            @PathVariable Long transactionPoid
    ) {
        try {
            byte[] pdf = importManifestService.printCheckPortCharges(transactionPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=import-manifest-bl-port-charges-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("error",e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }
}
