package com.asg.shipping.importmanifestbl.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.shipping.importManifestUpdate.dto.*;
import com.asg.shipping.importmanifestbl.dto.LoadEmailFaxRequestDto;
import com.asg.shipping.importmanifestbl.dto.ResendCanRequestDto;
import com.asg.shipping.importmanifestbl.dto.SendEdiEmailsRequestDto;
import com.asg.shipping.importmanifestbl.service.ImportManifestBlService;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;
import static com.asg.common.lib.security.util.UserContext.getCompanyPoid;
import static com.asg.common.lib.security.util.UserContext.getGroupPoid;

@RestController
@RequiredArgsConstructor
@RequestMapping("v1/import-manifest-bl")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "import-manifest-bl-controller", description = "Manage Import Manifest BL records")
public class ImportManifestController {

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

        ImportManifestBlRequestDto updated = importManifestBlService.updateImportManifestBl(id, dto, companyPoid, groupPoid);

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
            EmailVerificationResponseDto response = importManifestBlService.updateEmailVerification(request.getTransactionPoId(), request);
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
            ResendCanResponseDto response = importManifestBlService.resendCan(request.getTransactionPoId());
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
            SendEdiEmailsResponseDto response = importManifestBlService.sendEdiEmails(request.getTransactionPoId());
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
            LoadEmailFaxResponseDto response = importManifestBlService.loadEmailFax(request.getTransactionPoId(), null);
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
            Map<String, Object> response = importManifestBlService.list(filters, pageable);
            return success("Import Manifest BL list retrieved successfully", response);
        } catch (Exception e) {
            return internalServerError("Failed to retrieve Import Manifest BL list: " + e.getMessage());
        }
    }
}
