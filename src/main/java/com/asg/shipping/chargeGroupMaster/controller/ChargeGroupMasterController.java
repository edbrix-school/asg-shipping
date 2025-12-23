package com.asg.shipping.chargeGroupMaster.controller;


import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.chargeGroupMaster.dto.ChargeGroupMasterRequestDto;
import com.asg.shipping.chargeGroupMaster.dto.ChargeGroupMasterResponseDto;
import com.asg.shipping.chargeGroupMaster.service.ChargeGroupMasterService;
import com.asg.shipping.common.ApiResponse;
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

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/v1/charge-group-master")
@Tag(
        name = "Charge Group Master",
        description = "APIs for managing Shipping Charge Group Master"
)
public class ChargeGroupMasterController {

    private final ChargeGroupMasterService service;

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    @Operation(
            summary = "Create Charge Group Master",
            description = "Create a new Charge Group Master record",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully created Charge Group Master",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ChargeGroupMasterResponseDto .class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input parameters or validation error",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> createChargeGroupMaster(
            @Parameter(description = "Charge Group Master creation data", required = true)
            @Valid @RequestBody ChargeGroupMasterRequestDto dto) {
        ChargeGroupMasterResponseDto result = service.create(
                dto
        );
        log.info("Successfully created charge group  with id: {}", result.getChargeGroupPoid());
        return ApiResponse.success("Charge Group Master created successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{id}")
    @Operation(
            summary = "Update Charge Group Master",
            description = "Update an existing Charge Group Master record",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully updated Charge Group Master",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ChargeGroupMasterResponseDto .class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Charge Group Master not found",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input parameters or validation error",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> updateChargeGroupMaster(
            @Parameter(description = "Charge Group Master POID", required = true, example = "12345")
            @PathVariable Long id,
            @Parameter(description = "Charge Group Master update data", required = true)
            @Valid @RequestBody ChargeGroupMasterRequestDto dto) {
        ChargeGroupMasterResponseDto result = service.update(
                id,
                dto
        );
        log.info("Successfully updated charge group  with id: {}", result.getChargeGroupPoid());
        return ApiResponse.success("Charge Group Master updated successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{id}")
    @Operation(
            summary = "Get charge group master details",
            description = "Retrieve complete charge group master information by ID",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved charge group master",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ChargeGroupMasterResponseDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Charge group master not found",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> getChargeGroupMaster(
            @Parameter(description = "Charge group master POID", required = true, example = "12345")
            @PathVariable Long id) {
        log.info("Getting charge group master with id: {}", id);
        ChargeGroupMasterResponseDto charge = service.findById(id);
        log.info("Successfully retrieved charge group master with id: {}", id);
        return ApiResponse.success("Charge group master retrieved successfully", charge);
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete charge group master",
            description = "Soft delete a charge group master by setting DELETED flag to Y and ACTIVE to N",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully deleted charge group master",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Charge group master not found",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> deleteCharge(
            @Parameter(description = "Charge group master POID", required = true, example = "12345")
            @PathVariable Long id) {
        log.info("Deleting charge with id: {}", id);
        service.delete(id);
        log.info("Successfully deleted charge group master with id: {}", id);
        return ApiResponse.success("Charge group master deleted successfully");
    }

    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = """
                    - ### Filters:
                      Use either:
                      1. A single `GLOBALSEARCH` filter, OR
                      2. Any combination of specific fields (CHARGE_GROUP_CODE, CHARGE_GROUP_POID, CREATED_BY, etc.).
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
                      • { "searchField": "CHARGE_GROUP_CODE", "searchValue": "AGCI001" },
                    
                    - #### Single Field, Multiple Values:
                      Provide multiple values for the same field, separated by `|`.
                      • { "searchField": "CHARGE_GROUP_POID", "searchValue": 20 }
                    
                    - #### Multiple Different Fields, Single or Multiple Value:
                      Provide one or multiple search values across multiple fields.
                      • { "searchField": "CHARGE_GROUP_CODE", "searchValue": "AGCI001" },
                      • { "searchField": "CREATED_BY", "searchValue": "ADMIN|USER" }
                      • "operator" :  "AND"/"OR"
                    
                    - ### Sorting:
                      Defaults to whatever specified in list_of_records_sql field mostly primary key ascending.
                      To override, pass `sort=<field>,ASC|DESC` in query params.
                      Examples:
                      • sort=CHARGE_GROUP_CODE,ASC
                      • sort=CHARGE_GROUP_POID,DESC
                    """,
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "Charge Group Master Filters",
                                    value = """
                                            {
                                            "operator": "AND",
                                            "isDeleted": "N",
                                            "filters": [
                                               { "searchField": "GLOBALSEARCH", "searchValue": "United" },
                                               { "searchField": "CHARGE_GROUP_CODE", "searchValue": "AGCI001" },
                                               { "searchField": "CHARGE_GROUP_POID", "searchValue": 20},
                                               { "searchField": "CREATED_BY", "searchValue": "ADMIN"}
                                            ]
                                            }
                                            """
                            )
                    }
            )
    )
    @PostMapping("/list")
    public ResponseEntity<?> getChargeGroupMasterList(@ParameterObject Pageable pageable,
                                            @RequestBody(required = false) FilterRequestDto filters) {
        try {
            Map<String, Object> countries = service.listChargeGroupMaster(UserContext.getDocumentId(), filters, pageable);
            return success("Charge Group Master list fetched successfully", countries);
        } catch (Exception e) {
            return internalServerError("Error fetching Charge Group Master List: " + e.getMessage());
        }
    }


}
