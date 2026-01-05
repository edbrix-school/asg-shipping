package com.asg.shipping.importManifestUpdate.controller;


import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.response.ApiResponse;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.importManifestUpdate.dto.ImportManifestBlRequestDto;
import com.asg.shipping.importManifestUpdate.dto.ImportManifestBlUpdateDTO;
import com.asg.shipping.importManifestUpdate.service.ImportManifestBlService;
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

import static com.asg.common.lib.dto.response.ApiResponse.internalServerError;
import static com.asg.common.lib.dto.response.ApiResponse.success;
import static com.asg.common.lib.security.util.UserContext.getCompanyPoid;
import static com.asg.common.lib.security.util.UserContext.getGroupPoid;

@RestController
@RequestMapping("/v1/import-manifest-update-ops-bl")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Import Manifest Update OPS- BL Management", description = "APIs for managing Import Manifest BL records")
public class ImportManifestBlController {

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
            @PathVariable Long id) {

        log.info("Deleting Import Manifest BL with id: {}", id);
        service.deleteImportManifestBl(id);
        log.info("Successfully deleted Import Manifest BL with id: {}", id);
        return ApiResponse.success("Import Manifest BL deleted successfully");
    }
}
