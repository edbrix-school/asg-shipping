package com.asg.shipping.shippingFFChargeMaster.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;

import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.common.ApiResponse;
import com.asg.shipping.shippingFFChargeMaster.dto.ChargeCreateDTO;
import com.asg.shipping.shippingFFChargeMaster.dto.ChargeDto;
import com.asg.shipping.shippingFFChargeMaster.dto.ChargeUpdateDTO;
import com.asg.shipping.shippingFFChargeMaster.service.ShippingFFChargeMasterService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.internalServerError;
import static com.asg.common.lib.dto.response.ApiResponse.success;

@RestController
@RequestMapping("/v1/shipping-ff-charge-master")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Shipping/FF Charge Master Management", description = "APIs for managing shipping/FF charges")
public class ShippingFFChargeMasterController {

    private final ShippingFFChargeMasterService chargeMasterService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{id}")
    @Operation(
            summary = "Get charge details",
            description = "Retrieve complete charge information by ID",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved charge",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ChargeDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Charge not found",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> getCharge(
            @Parameter(description = "Charge POID", required = true, example = "12345")
            @PathVariable Long id) {
        log.info("Getting charge with id: {}", id);
        ChargeDto charge = chargeMasterService.getCharge(id);
        log.info("Successfully retrieved charge with id: {}", id);
        return ApiResponse.success("Charge retrieved successfully", charge);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    @Operation(
            summary = "Create charge",
            description = "Create a new charge record",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully created charge",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ChargeDto.class)
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
    public ResponseEntity<?> createCharge(
            @Parameter(description = "Charge creation data", required = true)
            @Valid @RequestBody ChargeCreateDTO dto) {
        log.info("Creating charge with code: {}, groupId: {}, userPoid: {}",
                dto.getChargeCode(), UserContext.getGroupPoid(), UserContext.getUserPoid());
        ChargeDto result = chargeMasterService.createCharge(
                dto,
                UserContext.getGroupPoid(),
                UserContext.getUserPoid()
        );
        log.info("Successfully created charge with id: {}", result.getChargePoid());
        return ApiResponse.success("Charge created successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{id}")
    @Operation(
            summary = "Update charge",
            description = "Update an existing charge record",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully updated charge",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ChargeDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Charge not found",
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
    public ResponseEntity<?> updateCharge(
            @Parameter(description = "Charge POID", required = true, example = "12345")
            @PathVariable Long id,
            @Parameter(description = "Charge update data", required = true)
            @Valid @RequestBody ChargeUpdateDTO dto) {
        log.info("Updating charge with id: {}, groupId: {}, userPoid: {}",
                id, UserContext.getGroupPoid(), UserContext.getUserPoid());
        ChargeDto result = chargeMasterService.updateCharge(
                id,
                dto,
                UserContext.getGroupPoid(),
                UserContext.getUserPoid()
        );
        log.info("Successfully updated charge with id: {}", id);
        return ApiResponse.success("Charge updated successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete charge",
            description = "Soft delete a charge by setting DELETED flag to Y and ACTIVE to N",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully deleted charge",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Charge not found",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> deleteCharge(
            @Parameter(description = "Charge POID", required = true, example = "12345")
            @PathVariable Long id) {
        log.info("Deleting charge with id: {}", id);
        chargeMasterService.deleteCharge(id);
        log.info("Successfully deleted charge with id: {}", id);
        return ApiResponse.success("Charge deleted successfully");
    }

    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = """
                    - ### Filters:
                      Use either:
                      1. A single `GLOBALSEARCH` filter, OR
                      2. Any combination of specific fields (CHARGE_NAME, CHARGE_CODE, CREATED_BY, etc.).
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
                      • { "searchField": "CHARGE_NAME", "searchValue": "Agency Fee" },
                    
                    - #### Single Field, Multiple Values:
                      Provide multiple values for the same field, separated by `|`.
                      • { "searchField": "CHARGE_CODE", "searchValue": "AGFE" }
                    
                    - #### Multiple Different Fields, Single or Multiple Value:
                      Provide one or multiple search values across multiple fields.
                      • { "searchField": "CHARGE_NAME", "searchValue": "Agency Fee" },
                      • { "searchField": "CREATED_BY", "searchValue": "ADMIN|USER" }
                      • "operator" :  "AND"/"OR"
                    
                    - ### Sorting:
                      Defaults to whatever specified in list_of_records_sql field mostly primary key ascending.
                      To override, pass `sort=<field>,ASC|DESC` in query params.
                      Examples:
                      • sort=CHARGE_NAME,ASC
                      • sort=CHARGE_CODE,DESC
                    """,
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "FF Charge Master Filters",
                                    value = """
                                            {
                                            "operator": "AND",
                                            "isDeleted": "N",
                                            "filters": [
                                               { "searchField": "GLOBALSEARCH", "searchValue": "United" },
                                               { "searchField": "CHARGE_NAME", "searchValue": "Agency Fee" },
                                               { "searchField": "CHARGE_CODE", "searchValue": "AGFE"},
                                               { "searchField": "CREATED_BY", "searchValue": "ADMIN"}
                                            ]
                                            }
                                            """
                            )
                    }
            )
    )
    @PostMapping("/search")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    public ResponseEntity<?> geSearchCharges(@ParameterObject Pageable pageable,
                                            @RequestBody(required = false) FilterRequestDto filters) {
        try {
            Map<String, Object> countries = chargeMasterService.searchCharges(UserContext.getDocumentId(), filters, pageable);
            return success("FF Charge Master List fetched successfully", countries);
        } catch (Exception e) {
            return internalServerError("Error fetching FF Charge Master List: " + e.getMessage());
        }
    }
}
