package com.asg.shipping.importmanifestbl.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.shipping.importManifestUpdate.dto.*;
import com.asg.shipping.importmanifestbl.service.ImportManifestBlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("v1/import-manifest-bl")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "import-manifest-bl-controller", description = "Manage Import Manifest BL records")
public class ImportManifestBlController {

    private final ImportManifestBlService importManifestBlService;

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
            ImportManifestBlRequestDto response = importManifestBlService.createImportManifestBl(request, companyPoid, groupPoid);
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
            ImportManifestBlRequestDto response = importManifestBlService.getImportManifest(id);
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
            importManifestBlService.delete(transactionPoId);
            return success("Import Manifest BL deleted successfully", null);
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to delete Import Manifest BL: " + e.getMessage());
        }
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
    @PostMapping("/{id}/update-email-verification")
    public ResponseEntity<?> updateEmailVerification(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody EmailVerificationRequestDto request
    ) {
        try {
            EmailVerificationResponseDto response = importManifestBlService.updateEmailVerification(id, request);
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
    @PostMapping("/{id}/resend-can")
    public ResponseEntity<?> resendCan(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long id
    ) {
        try {
            ResendCanResponseDto response = importManifestBlService.resendCan(id);
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
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{id}/send-edi-emails")
    public ResponseEntity<?> sendEdiEmails(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long id
    ) {
        try {
            SendEdiEmailsResponseDto response = importManifestBlService.sendEdiEmails(id);
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
    @PostMapping("/{id}/load-email-fax")
    public ResponseEntity<?> loadEmailFax(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody LoadEmailFaxRequestDto request
    ) {
        try {
            LoadEmailFaxResponseDto response = importManifestBlService.loadEmailFax(id, request);
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
            BlStatusResponseDto response = importManifestBlService.getBlStatus(id);
            return success("BL status retrieved successfully", response);
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to retrieve BL status: " + e.getMessage());
        }
    }
}
