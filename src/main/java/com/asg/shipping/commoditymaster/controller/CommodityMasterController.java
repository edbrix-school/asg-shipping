package com.asg.shipping.commoditymaster.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.commoditymaster.dto.request.CommodityMasterRequest;
import com.asg.shipping.commoditymaster.dto.response.CommodityMasterResponse;
import com.asg.shipping.commoditymaster.service.CommodityMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@RestController
@RequestMapping("/v1/cargo-commodity-master")
@RequiredArgsConstructor
public class CommodityMasterController {

    private final CommodityMasterService commodityMasterService;
    private final LoggingService loggingService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "List Commodities with Search and Sort",
            description = "Provide search filters. Valid `searchField` values: GLOBALSEARCH or (COMODITY_CODE, COMODITY_NAME, COMODITY_NAME2). Sorting default on commodityPoid, desc."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = """
                    - ### Filters:
                      Use either:
                      1. A single `GLOBALSEARCH` filter, OR
                      2. Any combination of specific fields (COMODITY_CODE, COMODITY_NAME, COMODITY_NAME2).
                      3. operator field will either have "AND" or "OR", if not given will be considered as "OR"
                      4. isDeleted when 'N' or null, will search and return non deleted records, 'Y' will check and return deleted records
                    """,
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "Commodity Filters",
                                    value = """
                                             {
                                             "operator": "OR",
                                             "isDeleted": "N",
                                             "filters": [
                                                { "searchField": "GLOBALSEARCH", "searchValue": "CONTAINER" },
                                                { "searchField": "COMODITY_CODE", "searchValue": "CONT001" },
                                                { "searchField": "COMODITY_NAME", "searchValue": "CONTAINER CARGO" },
                                                { "searchField": "COMODITY_NAME2", "searchValue": "CONTAINERS" }
                                             ]
                                             }
                                            """
                            )
                    }
            )
    )
    @PostMapping("/list")
    public ResponseEntity<?> getCommodities(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters) {
        try {
            Map<String, Object> commodities = commodityMasterService.listCommodities(UserContext.getDocumentId(), filters, pageable);
            return success("Commodity list fetched successfully", commodities);
        } catch (Exception e) {
            return internalServerError("Unable to fetch commodity list: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Get Commodity by ID", description = "Retrieve a specific commodity by its ID")
    @GetMapping("/{commodityPoid}")
    public ResponseEntity<?> getCommodityById(@PathVariable Long commodityPoid) {
        try {
            CommodityMasterResponse commodity = commodityMasterService.getCommodityById(commodityPoid);
            loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), commodityPoid.toString());
            return success("Commodity fetched successfully", commodity);
        } catch (Exception e) {
            return internalServerError("Failed to retrieve commodity: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @Operation(
            summary = "Create Commodity",
            description = "Create a new commodity based on the request payload."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = """      
                    ### Request Body
                        Provide commodity details.
                        - **commodityPoid:** Must be `null` when creating.
                        - **commodityName:** (Required) Name of the commodity (e.g., Container Cargo).
                        - Other fields as applicable for commodity setup
                    """,
            content = @Content(
                    schema = @Schema(implementation = CommodityMasterRequest.class),
                    examples = {
                            @ExampleObject(
                                    name = "Commodity Create Example",
                                    value = """
                                            {
                                              "commodityName": "Container Cargo",
                                              "commodityName2": "Containers",
                                              "seqno": 1,
                                              "active": "Y"
                                            }
                                            """
                            )
                    }
            )
    )
    @PostMapping("/")
    public ResponseEntity<?> createCommodity(@RequestBody @Valid CommodityMasterRequest request) {
        try {
            if (request.getCommodityPoid() != null) {
                return badRequest("commodityPoid must be null when creating");
            }

            CommodityMasterResponse savedCommodity = commodityMasterService.createCommodity(request);
            return success("Commodity created successfully", savedCommodity);
        } catch (Exception ex) {
            return internalServerError(ex.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(
            summary = "Update Commodity",
            description = "Update an existing commodity based on the request payload."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = """
                    ### Request Body
                        Provide commodity details.
                        - **commodityName:** (Required) Name of the commodity (e.g., Container Cargo).
                        - Other fields as applicable for commodity setup
                    """,
            content = @Content(
                    schema = @Schema(implementation = CommodityMasterRequest.class),
                    examples = {
                            @ExampleObject(
                                    name = "Commodity Update Example",
                                    value = """
                                            {
                                              "commodityName": "Container Cargo Updated",
                                              "commodityName2": "Containers",
                                              "seqno": 1,
                                              "active": "Y"
                                            }
                                            """
                            )
                    }
            )
    )
    @PutMapping("/{commodityPoid}")
    public ResponseEntity<?> updateCommodity(
            @PathVariable Long commodityPoid,
            @RequestBody @Valid CommodityMasterRequest request) {
        try {
            CommodityMasterResponse updatedCommodity = commodityMasterService.updateCommodity(commodityPoid, request);
            return success("Commodity updated successfully", updatedCommodity);
        } catch (Exception ex) {
            return internalServerError(ex.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @Operation(
            summary = "Soft Delete Commodity",
            description = """
                    Soft deletes a commodity based on the provided parameters.
                    
                    ### Request Parameters
                        - **commodityPoid:** Commodity's Primary Key
                    """
    )
    @DeleteMapping("/{commodityPoid}")
    public ResponseEntity<?> softDeleteCommodity(
            @PathVariable Long commodityPoid,
            @Valid @RequestBody(required = false) com.asg.common.lib.dto.DeleteReasonDto deleteReasonDto) {
        try {
            commodityMasterService.softDeleteCommodity(commodityPoid, deleteReasonDto);
            return success("Commodity soft deleted successfully", Map.of("commodityPoid", commodityPoid));
        } catch (Exception e) {
            return internalServerError("Failed to soft delete commodity: " + e.getMessage());
        }
    }
}