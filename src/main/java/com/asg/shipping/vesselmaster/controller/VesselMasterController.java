package com.asg.shipping.vesselmaster.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.shipping.common.ApiResponse;
import com.asg.shipping.vesselmaster.dto.*;
import com.asg.shipping.vesselmaster.service.VesselMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for Vessel Master operations
 */
@RestController
@RequestMapping("/v1/vessel-master")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Vessel Master Management", description = "APIs for managing shipping vessels")
public class VesselMasterController {

    private final VesselMasterService vesselService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    @Operation(
            summary = "List vessels",
            description = "Retrieve paginated list of vessels with optional filtering and sorting using DocumentSearchService",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
            @Parameter(
                    name = "X-Document-Id",
                    in = ParameterIn.HEADER,
                    description = "Document identifier for auditing",
                    example = "100-008",
                    required = true,
                    schema = @Schema(type = "string", example = "100-008")
            ),
            @Parameter(
                    name = "X-Action-Requested",
                    in = ParameterIn.HEADER,
                    description = "Action requested must match this endpoint's @AllowedAction (VIEW).",
                    example = "VIEW",
                    required = true,
                    schema = @Schema(type = "string", example = "VIEW")
            )
    })
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved vessels",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> searchVessels(
            @RequestBody(required = false) com.asg.common.lib.dto.FilterRequestDto request,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field and direction (e.g., 'vesselName,asc')", example = "vesselName,asc")
            @RequestParam(required = false) String sort) {

        log.info("Searching vessels with page: {}, size: {}, sort: {}", page, size, sort);

        Pageable pageable = createPageable(page, size, sort);
        Map<String, Object> result = vesselService.searchVessels(request, pageable);

        log.info("Successfully retrieved vessels");
        return ApiResponse.success("Vessels retrieved successfully", result);
    }



    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{id}")
    @Operation(
            summary = "Get vessel details",
            description = "Retrieve complete vessel information by ID",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
            @Parameter(
                    name = "X-Document-Id",
                    in = ParameterIn.HEADER,
                    description = "Document identifier required for auditing purposes.",
                    example = "100-008",
                    required = true,
                    schema = @Schema(type = "string", example = "100-008")

            ),
            @Parameter(
                    name = "X-Action-Requested",
                    in = ParameterIn.HEADER,
                    description = "Action requested must match this endpoint's @AllowedAction (VIEW).",
                    example = "VIEW",
                    required = true,
                    schema = @Schema(type = "string", example = "VIEW")
            )
    })
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved vessel",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = VesselMasterDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Vessel not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> getVessel(
            @Parameter(description = "Vessel POID", required = true, example = "12345")
            @PathVariable Long id) {
        log.info("Getting vessel with id: {}", id);
        VesselMasterDto vessel = vesselService.getVessel(id);
        log.info("Successfully retrieved vessel with id: {}", id);
        return ApiResponse.success("Vessel retrieved successfully", vessel);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    @Operation(
            summary = "Create vessel",
            description = "Create a new vessel record",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
            @Parameter(
                    name = "X-Document-Id",
                    in = ParameterIn.HEADER,
                    description = "Document identifier required for auditing purposes.",
                    example = "100-008",
                    required = true,
                    schema = @Schema(type = "string", example = "100-008")
            ),
            @Parameter(
                    name = "X-Action-Requested",
                    in = ParameterIn.HEADER,
                    description = "Action requested must match this endpoint's @AllowedAction (CREATE).",
                    example = "CREATE",
                    required = true,
                    schema = @Schema(type = "string", example = "CREATE")
            )
    })
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully created vessel",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = VesselMasterDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input parameters or validation error",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "IMO number already exists",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> createVessel(
            @Parameter(description = "Vessel creation data", required = true)
            @Valid @RequestBody VesselMasterCreateDTO dto) {
        log.info("Creating vessel with code: {}, name: {}", dto.getVesselCode(), dto.getVesselName());
        VesselMasterDto result = vesselService.createVessel(dto);
        log.info("Successfully created vessel with id: {}", result.getVesselPoid());
        return ApiResponse.success("Vessel created successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{id}")
    @Operation(
            summary = "Update vessel",
            description = "Update an existing vessel record. Note: VESSEL_CODE is not updateable (updateable only on insert).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
            @Parameter(
                    name = "X-Document-Id",
                    in = ParameterIn.HEADER,
                    description = "Document identifier required for auditing purposes.",
                    example = "100-008",
                    required = true,
                    schema = @Schema(type = "string", example = "100-008")
            ),
            @Parameter(
                    name = "X-Action-Requested",
                    in = ParameterIn.HEADER,
                    description = "Action requested must match this endpoint's @AllowedAction (EDIT).",
                    example = "EDIT",
                    required = true,
                    schema = @Schema(type = "string", example = "EDIT")
            )
    })
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully updated vessel",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = VesselMasterDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Vessel not found",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input parameters or validation error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> updateVessel(
            @Parameter(description = "Vessel POID", required = true, example = "12345")
            @PathVariable Long id,
            @Parameter(description = "Vessel update data", required = true)
            @Valid @RequestBody VesselMasterUpdateDTO dto) {
        log.info("Updating vessel with id: {}", id);
        VesselMasterDto result = vesselService.updateVessel(id, dto);
        log.info("Successfully updated vessel with id: {}", id);
        return ApiResponse.success("Vessel updated successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PatchMapping("/{id}/activate")
    @Operation(
            summary = "Toggle active status",
            description = "Toggle the active flag of a vessel between Y and N",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
            @Parameter(
                    name = "X-Document-Id",
                    in = ParameterIn.HEADER,
                    description = "Document identifier required for auditing purposes.",
                    example = "100-008",
                    required = true,
                    schema = @Schema(type = "string", example = "100-008")
            ),
            @Parameter(
                    name = "X-Action-Requested",
                    in = ParameterIn.HEADER,
                    description = "Action requested must match this endpoint's @AllowedAction (EDIT).",
                    example = "EDIT",
                    required = true,
                    schema = @Schema(type = "string", example = "EDIT")
            )
    })
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully toggled vessel status",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Vessel not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> toggleActive(
            @Parameter(description = "Vessel POID", required = true, example = "12345")
            @PathVariable Long id) {
        log.info("Toggling active status for vessel with id: {}", id);
        vesselService.toggleActive(id);
        log.info("Successfully toggled active status for vessel with id: {}", id);
        return ApiResponse.success("Vessel status toggled successfully");
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete vessel",
            description = "Soft delete a vessel by setting DELETED flag to Y and ACTIVE to N",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
            @Parameter(
                    name = "X-Document-Id",
                    in = ParameterIn.HEADER,
                    description = "Document identifier required for auditing purposes.",
                    example = "100-008",
                    required = true,
                    schema = @Schema(type = "string", example = "100-008")
            ),
            @Parameter(
                    name = "X-Action-Requested",
                    in = ParameterIn.HEADER,
                    description = "Action requested must match this endpoint's @AllowedAction (DELETE).",
                    example = "DELETE",
                    required = true,
                    schema = @Schema(type = "string", example = "DELETE")
            )
    })
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully deleted vessel",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Vessel not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> deleteVessel(
            @Parameter(description = "Vessel POID", required = true, example = "12345")
            @PathVariable Long id,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {
        log.info("Deleting vessel with id: {}", id);
        vesselService.deleteVessel(id, deleteReasonDto);
        log.info("Successfully deleted vessel with id: {}", id);
        return ApiResponse.success("Vessel deleted successfully");
    }

    /**
     * Create Pageable from page, size, and sort parameters
     */
    private Pageable createPageable(int page, int size, String sort) {
        if (sort != null && !sort.isEmpty()) {
            String[] sortParts = sort.split(",");
            if (sortParts.length == 2) {
                String sortField = sortParts[0].trim();
                String sortDirection = sortParts[1].trim().toUpperCase();
                
                String dbColumn = mapSortFieldToColumn(sortField);
                Sort.Direction direction = "DESC".equals(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
                Sort sortObj = Sort.by(direction, dbColumn);
                return PageRequest.of(page, size, sortObj);
            }
        }
        Sort defaultSort = Sort.by(Sort.Direction.ASC, "VESSEL_NAME");
        return PageRequest.of(page, size, defaultSort);
    }

    /**
     * Map frontend sort field names to database column names
     */
    private String mapSortFieldToColumn(String sortField) {
        if (sortField == null) {
            return "VESSEL_NAME";
        }
        
        String normalized = sortField.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
        
        switch (normalized) {
            case "VESSEL_POID":
                return "VESSEL_POID";
            case "VESSEL_CODE":
                return "VESSEL_CODE";
            case "VESSEL_NAME":
                return "VESSEL_NAME";
            case "VESSEL_NAME2":
                return "VESSEL_NAME2";
            case "IMO_NUMBER":
                return "IMO_NUMBER";
            case "CREATED_DATE":
                return "CREATED_DATE";
            case "LASTMODIFIED_DATE":
                return "LASTMODIFIED_DATE";
            default:
                return "VESSEL_NAME";
        }
    }
}

