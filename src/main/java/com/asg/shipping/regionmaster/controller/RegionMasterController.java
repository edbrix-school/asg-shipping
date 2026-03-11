package com.asg.shipping.regionmaster.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.regionmaster.dto.RegionMasterRequest;
import com.asg.shipping.regionmaster.dto.RegionMasterResponse;
import com.asg.shipping.regionmaster.service.RegionMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import static com.asg.shipping.common.ApiResponse.internalServerError;
import static com.asg.shipping.common.ApiResponse.success;

@RestController
@RequestMapping("/v1/region-master")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Region Master", description = "APIs for managing Region Master")
public class RegionMasterController {

    private final RegionMasterService regionMasterService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "Search/List Region Masters",
            description = """
                    Retrieve a paginated list of region masters with optional filtering and sorting.
                    Uses DocumentSearchService for unified search functionality.
                    
                    Valid `searchField` values:
                    - GLOBALSEARCH
                    - REGION_CODE
                    - REGION_NAME
                    - ACTIVE

                    Sorting will be applied as specified in list_of_records_sql
                    configured in doc_master table.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            description = """
                    - ### Filters:
                      Use either:
                      1. A single `GLOBALSEARCH` filter, OR
                      2. Any combination of specific fields
                      3. operator can be AND / OR (default OR)
                      4. isDeleted:
                         - N or null → non-deleted
                         - Y → deleted records
                    """,
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "Region Master Filters",
                                    value = """
                                            {
                                              "operator": "OR",
                                              "isDeleted": "N",
                                              "filters": [
                                                { "searchField": "GLOBALSEARCH", "searchValue": "Middle" },
                                                { "searchField": "REGION_CODE", "searchValue": "ME" },
                                                { "searchField": "REGION_NAME", "searchValue": "Middle East" }
                                              ]
                                            }
                                            """
                            )
                    }
            )
    )
    @PostMapping("/search")
    public ResponseEntity<?> searchRegionMasters(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters) {

        try {
            Long groupPoid = UserContext.getGroupPoid();
            String docId = UserContext.getDocumentId();
            
            log.info("Search Region Masters request | page={}, size={}, docId={}, groupPoid={}",
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    docId,
                    groupPoid);
            
            Map<String, Object> result =
                    regionMasterService.listRegionMasters(
                            docId,
                            filters,
                            pageable
                    );
            
            log.info("Search Region Masters completed successfully | totalRecords={}", 
                    result.get("totalElements"));
            
            return success("Region masters retrieved successfully", result);
        } catch (Exception e) {
            log.error("Error searching region masters", e);
            return internalServerError("Unable to fetch region masters: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{regionPoid}")
    @Operation(
            summary = "Get Region Master by ID",
            description = "Retrieve region master details using POID and Group POID",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getById(
            @PathVariable @NotNull @Positive Long regionPoid) {
        
        Long groupPoid = UserContext.getGroupPoid();
        
        log.info("Get Region Master request | regionPoid={}, groupPoid={}",
                regionPoid,
                groupPoid);
        
        RegionMasterResponse response =
                regionMasterService.getById(regionPoid, groupPoid);

        log.info("Get Region Master completed successfully | regionPoid={}", regionPoid);

        return success(
                "Region master retrieved successfully",
                response
        );
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    @Operation(
            summary = "Create Region Master",
            description = "Creates a new region master record. Validates uniqueness of region code and name.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> create(
            @Valid @RequestBody RegionMasterRequest request) {
        
        Long groupPoid = UserContext.getGroupPoid();
        String userId = UserContext.getUserId();
        String docId = UserContext.getDocumentId();
        
        log.info("Create Region Master request | code={}, name={}, groupPoid={}, userId={}",
                request.getRegionCode(),
                request.getRegionName(),
                groupPoid,
                userId);
        
        RegionMasterResponse response =
                regionMasterService.create(request, groupPoid, userId, docId);

        log.info("Create Region Master completed successfully | regionPoid={}", 
                response.getRegionPoid());

        return success(
                "Region master created successfully",
                response
        );
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{regionPoid}")
    @Operation(
            summary = "Update Region Master",
            description = "Updates an existing region master record. Validates uniqueness of region code and name if changed.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> update(
            @PathVariable @NotNull @Positive Long regionPoid,
            @Valid @RequestBody RegionMasterRequest request) {
        
        Long groupPoid = UserContext.getGroupPoid();
        String userId = UserContext.getUserId();
        String docId = UserContext.getDocumentId();
        
        log.info("Update Region Master request | regionPoid={}, code={}, name={}, groupPoid={}, userId={}",
                regionPoid,
                request.getRegionCode(),
                request.getRegionName(),
                groupPoid,
                userId);

        RegionMasterResponse response =
                regionMasterService.update(regionPoid, request, groupPoid, userId, docId);

        log.info("Update Region Master completed successfully | regionPoid={}", regionPoid);

        return success(
                "Region master updated successfully",
                response
        );
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{regionPoid}/activate")
    @Operation(
            summary = "Toggle Active Status",
            description = "Toggles the active status of a region master between Y and N",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> toggleActiveStatus(
            @PathVariable @NotNull @Positive Long regionPoid) {
        
        Long groupPoid = UserContext.getGroupPoid();
        String userId = UserContext.getUserId();
        
        log.info("Toggle Active Status request | regionPoid={}, groupPoid={}, userId={}",
                regionPoid,
                groupPoid,
                userId);
        
        regionMasterService.toggleActiveStatus(regionPoid, groupPoid, userId);
        
        log.info("Toggle Active Status completed successfully | regionPoid={}", regionPoid);
        
        return success("Region master status toggled successfully");
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{regionPoid}")
    @Operation(
            summary = "Delete Region Master",
            description = "Soft deletes a region master by setting DELETED='Y' and ACTIVE='N'",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> delete(@PathVariable @NotNull @Positive Long regionPoid,@RequestBody DeleteReasonDto deleteReasonDto) {
        
        Long groupPoid = UserContext.getGroupPoid();
        String userId = UserContext.getUserId();
        
        log.info("Delete Region Master request | regionPoid={}, groupPoid={}, userId={}",
                regionPoid,
                groupPoid,
                userId);
        
        regionMasterService.delete(regionPoid, deleteReasonDto);
        
        log.info("Delete Region Master completed successfully | regionPoid={}", regionPoid);
        
        return success("Region master deleted successfully");
    }
}

