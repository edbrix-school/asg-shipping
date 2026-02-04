package com.asg.shipping.exportManifestBl.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.response.ApiResponse;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.exportManifestBl.dto.*;
import com.asg.shipping.exportManifestBl.service.ExportManifestBlService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;
import static com.asg.common.lib.security.util.UserContext.getCompanyPoid;
import static com.asg.common.lib.security.util.UserContext.getGroupPoid;

@RestController
@RequestMapping("/v1/export-manifest-bl")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Export Manifest BL Management", description = "APIs for managing Export Manifest BL records")
public class ExportManifestBlController {

    private final ExportManifestBlService service;

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
                                    name = "Export Manifest Filters",
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
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/List")
    @Operation(
            summary = "Search Export Manifest BL records",
            description = "Search Export Manifest BL records with pagination, filtering, and sorting. Only returns records where BL_TYPE = 'EXPORT'.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved Export Manifest BL list",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Insufficient permissions",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> searchExportManifestBl(
            @Parameter(description = "Document ID for search configuration", required = true, example = "TBD")
            @RequestParam(required = false) String docId,
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters) {
        try {
            String documentId = docId != null ? docId : UserContext.getDocumentId();
            Map<String, Object> result = service.searchExportManifestBl(documentId, filters, pageable);
            return success("Export Manifest BL list fetched successfully", result);
        } catch (Exception e) {
            log.error("Error fetching Export Manifest BL List", e);
            return internalServerError("Error fetching Export Manifest BL List: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    @Operation(
            summary = "Get Export Manifest BL record details",
            description = "Retrieve complete Export Manifest BL record information by ID including all detail records",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved Export Manifest BL record",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ExportManifestBlRequestDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Export Manifest BL record not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> getExportManifestBl(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long transactionPoid) {

        log.info("Getting Export Manifest BL with transactionPoid: {}", transactionPoid);
        ExportManifestBlRequestDto manifestBl = service.getExportManifestBl(transactionPoid);
        log.info("Successfully retrieved Export Manifest BL with transactionPoid: {}", transactionPoid);
        return ApiResponse.success("Export Manifest BL retrieved successfully", manifestBl);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    @Operation(
            summary = "Create Export Manifest BL record",
            description = "Create a new Export Manifest BL record with validation. BL_TYPE is automatically set to 'EXPORT'.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully created Export Manifest BL record",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ExportManifestBlRequestDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or validation error",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "Business rule violation (e.g., BL number already exists)",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> createExportManifestBl(
            @Valid @RequestBody ExportManifestBlCreateDto dto) {

        log.info("Creating new Export Manifest BL");
        ExportManifestBlRequestDto created = service.createExportManifestBl(dto);
        log.info("Successfully created Export Manifest BL with transactionPoid: {}", created.getTransactionPoid());
        return ApiResponse.success("Export Manifest BL created successfully", created);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}")
    @Operation(
            summary = "Update Export Manifest BL record",
            description = "Update an existing Export Manifest BL record with validation",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully updated Export Manifest BL record",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ExportManifestBlRequestDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or validation error",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Export Manifest BL record not found",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "Business rule violation",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> updateExportManifestBl(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long transactionPoid,
            @Valid @RequestBody ExportManifestBlUpdateDto dto) {

        log.info("Updating Export Manifest BL with transactionPoid: {}", transactionPoid);

        Long companyPoid = getCompanyPoid();
        Long groupPoid = getGroupPoid();

        ExportManifestBlRequestDto updated = service.updateExportManifestBl(transactionPoid, dto, companyPoid, groupPoid);

        log.info("Successfully updated Export Manifest BL with transactionPoid: {}", transactionPoid);
        return ApiResponse.success("Export Manifest BL updated successfully", updated);
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}")
    @Operation(
            summary = "Delete Export Manifest BL record",
            description = "Soft delete an Export Manifest BL record by setting DELETED='Y'",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully deleted Export Manifest BL record"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Export Manifest BL record not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> deleteExportManifestBl(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long transactionPoid) {

        log.info("Deleting Export Manifest BL with transactionPoid: {}", transactionPoid);
        service.deleteExportManifestBl(transactionPoid);
        log.info("Successfully deleted Export Manifest BL with transactionPoid: {}", transactionPoid);
        return ApiResponse.success("Export Manifest BL deleted successfully");
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/ff-job/{blNumber}")
    @Operation(
            summary = "Get FF job details by BL number",
            description = "Retrieve FF job details from VW_SHIP_BL_TO_FF view based on BL number",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved FF job details",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ShipBlToFfDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "FF job details not found for the given BL number",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> getShipBlToFfByBlNumber(
            @Parameter(description = "BL Number", required = true, example = "HJSCSHZJ40741500")
            @PathVariable String blNumber) {

        log.info("Getting FF job details for BL number: {}", blNumber);
        ShipBlToFfDto result = service.getShipBlToFfByBlNumber(blNumber);
        log.info("Successfully retrieved FF job details for BL number: {}", blNumber);
        return ApiResponse.success("FF job details retrieved successfully", result);
    }
}


