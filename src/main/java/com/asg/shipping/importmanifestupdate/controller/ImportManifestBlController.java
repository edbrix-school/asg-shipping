package com.asg.shipping.importmanifestupdate.controller;


import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.response.ApiResponse;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.importmanifestupdate.dto.*;
import com.asg.shipping.importmanifestupdate.service.ImportManifestBlService;
import com.asg.shipping.importmanifestbl.dto.ResendCanRequestDto;
import com.asg.shipping.importmanifestbl.service.ImportManifestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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

import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;
import static com.asg.common.lib.dto.response.ApiResponse.notFound;
import static com.asg.common.lib.security.util.UserContext.getCompanyPoid;
import static com.asg.common.lib.security.util.UserContext.getGroupPoid;

@RestController
@RequestMapping("/v1/import-manifest-update-ops-bl")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Import Manifest Update OPS- BL Management", description = "APIs for managing Import Manifest BL records")
public class ImportManifestBlController {

    private final LoggingService loggingService;
    private final ImportManifestService manifestService;
    private final ImportManifestBlService service;

    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = """
                    - ### Filters:
                      Use either:
                      1. A single `GLOBALSEARCH` filter, OR
                      2. Any combination of specific fields (BL_NUMBER, TRANSACTION_POID, CREATED_BY, etc.).
                      3. operator field will either have "AND" or "OR", if not given will be considered as "OR",
                         not required for GLOBALSEARCH, for non GLOBALSEARCH need to give only 1 time
                      4. isDeleted when 'Y' or null, will search and return non deleted records, 'Y' will check and return deleted records
                      5. sort will be default on primary key ascending if specified in db field otherwise you can override it giving the field name and the direction.
                    
                    - #### Global Search:
                      Apply one search term across multiple fields.
                      No operator need to send in this case.
                      • { "searchField": "GLOBALSEARCH", "searchValue": "United" }
                    
                    - #### Single Field, Single Value:
                      Search one field with one value.
                      • { "searchField": "BL_NUMBER", "searchValue": "HJSCSHZJ40741500" },
                    
                    - #### Single Field, Multiple Values:
                      Provide multiple values for the same field, separated by `|`.
                      • { "searchField": "TRANSACTION_POID", "searchValue": 242834 }
                    
                    - #### Multiple Different Fields, Single or Multiple Value:
                      Provide one or multiple search values across multiple fields.
                      • { "searchField": "BL_NUMBER", "searchValue": "HJSCSHZJ40741500" },
                      • { "searchField": "CREATED_BY", "searchValue": "ADMIN|USER" }
                      • "operator" :  "AND"/"OR"
                    
                    - ### Sorting:
                      Defaults to whatever specified in list_of_records_sql field mostly primary key ascending.
                      To override, pass `sort=<field>,ASC|DESC` in query params.
                      Examples:
                      • sort=BL_NUMBER,ASC
                      • sort=TRANSACTION_POID,DESC
                    """,
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "Import Manifest Filters",
                                    value = """
                                            {
                                            "operator": "AND",
                                            "isDeleted": "N",
                                            "filters": [
                                               { "searchField": "GLOBALSEARCH", "searchValue": "United" },
                                               { "searchField": "BL_NUMBER", "searchValue": "HJSCSHZJ40741500" },
                                               { "searchField": "TRANSACTION_POID", "searchValue": 242834},
                                               { "searchField": "CREATED_BY", "searchValue": "ADMIN"}
                                            ]
                                            }
                                            """
                            )
                    }
            )
    )
    @PostMapping("/list")
    public ResponseEntity<?> getImportManifestList(@ParameterObject Pageable pageable,
                                                      @RequestBody(required = false) FilterRequestDto filters) {
        try {
            Map<String, Object> countries = service.listOfImportManifest(UserContext.getDocumentId(), filters, pageable);
            return success("Import Manifest list fetched successfully", countries);
        } catch (Exception e) {
            return internalServerError("Error fetching Import Manifest List: " + e.getMessage());
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

        log.info("Updating Import Manifest BL with id: {}", id);

        Long companyPoid = getCompanyPoid();
        Long groupPoid = getGroupPoid();

        ImportManifestBlRequestDto updated = service.updateImportManifestBl(id, dto, companyPoid, groupPoid);

        log.info("Successfully updated Import Manifest BL with id: {}", id);
        return ApiResponse.success("Import Manifest BL updated successfully", updated);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{id}")
    @Operation(
            summary = "Get Import Manifest BL record details",
            description = "Retrieve complete Import Manifest BL record information by ID including LOV data",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved Import Manifest BL record",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ImportManifestBlRequestDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Import Manifest BL record not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> getImportManifestBl(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long id) {

        log.info("Getting Import Manifest BL with id: {}", id);
        ImportManifestBlRequestDto manifestBl = service.getImportManifestBl(id);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), id.toString());
        log.info("Successfully retrieved Import Manifest BL with id: {}", id);
        return ApiResponse.success("Import Manifest BL retrieved successfully", manifestBl);
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete Import Manifest BL record",
            description = "Soft delete an Import Manifest BL record by setting DELETED='Y'",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully deleted Import Manifest BL record"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Import Manifest BL record not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> deleteImportManifestBl(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long id, @Valid @RequestBody
            DeleteReasonDto deleteReasonDto) {

        log.info("Deleting Import Manifest BL with id: {}", id);
        service.deleteImportManifestBl(id,deleteReasonDto);
        log.info("Successfully deleted Import Manifest BL with id: {}", id);
        return ApiResponse.success("Import Manifest BL deleted successfully");
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(
            summary = "Update Email Verification",
            description = "Update email verification status for Import Manifest BL."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Email verification updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Import Manifest BL not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/update-email-verification")
    public ResponseEntity<?> updateEmailVerification(
            @Valid @RequestBody EmailVerificationRequestDto request
    ) {
        try {
            EmailVerificationResponseDto response = service.updateEmailVerification(request.getTransactionPoId(), request);
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "CAN resent successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Import Manifest BL not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/resend-can")
    public ResponseEntity<?> resendCan(
            @Valid @RequestBody ResendCanRequestDto request
    ) {
        try {
            ResendCanResponseDto response = service.resendCan(request.getTransactionPoId(), request.getUpdateDemurrage());
            return success("CAN resent successfully", response);
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to resend CAN: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(
            summary = "Get EDI Emails",
            description = "Get EDI emails for Import Manifest BL."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "EDI emails retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Import Manifest BL not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}/send-edi-emails")
    public ResponseEntity<?> sendEdiEmails(
            @PathVariable Long id
    ) {
        try {
            SendEdiEmailsResponseDto response = service.sendEdiEmails(id);
            return success("EDI emails retrieved successfully", response);
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to retrieve EDI emails: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(
            summary = "Load Email/Fax Data",
            description = "Load email/fax data for selected party."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Email/Fax data loaded successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Import Manifest BL not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/load-email-fax")
    public ResponseEntity<?> loadEmailFax(
            @RequestParam Long addressMasterPoid,
            @RequestParam String addressType
    ) {
        try {
            LoadEmailFaxResponseDto response = service.loadEmailFax(addressMasterPoid, addressType);
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "BL status retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Import Manifest BL not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}/bl-status")
    public ResponseEntity<?> getBlStatus(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long id
    ) {
        try {
            BlStatusResponseDto response = service.getBlStatus(id);
            return success("BL status retrieved successfully", response);
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to retrieve BL status: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @GetMapping("/cargo-arrival-notice/{transactionPoid}")
    public ResponseEntity<?> printCargoArrivalNotice(
            @Parameter(description = "Transaction POID", example = "12345")
            @PathVariable Long transactionPoid,
            @Parameter(description = "Voyage Transaction POID", example = "67890", required = true)
            @RequestParam Long voyageTransactionPoid
    ) {
        try {
            byte[] pdf = manifestService.printCargoArrivalNotice(voyageTransactionPoid, transactionPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=import-manifest-update-bl-cargo-arrival-notice-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("error", e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @GetMapping("/uncleared-cargo-notice/{transactionPoid}")
    public ResponseEntity<?> printUnclearedCargoNotice(
            @Parameter(description = "Transaction POID", example = "12345")
            @PathVariable Long transactionPoid
    ) {
        try {
            byte[] pdf = manifestService.printUnclearedCargoNotice(transactionPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=import-manifest-update-bl-uncleared-cargo-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {

            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }


    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @GetMapping("/cargo-manifest-print/{transactionPoid}")
    public ResponseEntity<?> printCargoManifest(
            @Parameter(description = "Transaction POID", example = "12345")
            @PathVariable Long transactionPoid,
            @Parameter(description = "Is Cargo Manifest Print", example = "true")
            @RequestParam(defaultValue = "false") boolean isCargoManifestPrint
    ) {
        try {
            byte[] pdf = manifestService.printCargoManifest(transactionPoid, isCargoManifestPrint);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=import-manifest-update-bl-cargo-manifest-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("error",e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }
}
