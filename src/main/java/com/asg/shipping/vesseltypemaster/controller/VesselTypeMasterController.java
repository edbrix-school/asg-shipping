package com.asg.shipping.vesseltypemaster.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.common.ApiResponse;
import com.asg.shipping.vesseltypemaster.dto.VesselTypeCreateDTO;
import com.asg.shipping.vesseltypemaster.dto.VesselTypeDto;
import com.asg.shipping.vesseltypemaster.dto.VesselTypeUpdateDTO;
import com.asg.shipping.vesseltypemaster.service.VesselTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Pageable;
import java.util.Map;

@RestController
@RequestMapping("/v1/vessel-type-master")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Vessel Type Management", description = "APIs for managing vessel types")
public class VesselTypeMasterController {

        private final VesselTypeService vesselTypeService;

        @AllowedAction(UserRolesRightsEnum.VIEW)
        @PostMapping("/search")
        @Operation(
                summary = "Search vessel types",
                description = "Retrieve paginated list of vessel types with optional filtering and sorting using DocumentSearchService",
                security = @SecurityRequirement(name = "bearerAuth")
        )
        @ApiResponses(value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Successfully retrieved vessel types",
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
        public ResponseEntity<?> searchVesselTypes(
                @RequestBody(required = false) com.asg.common.lib.dto.FilterRequestDto request,
                @Parameter(description = "Page number (0-indexed)", example = "0")
                @RequestParam(defaultValue = "0") int page,
                @Parameter(description = "Page size", example = "20")
                @RequestParam(defaultValue = "20") int size,
                @Parameter(description = "Sort field and direction (e.g., 'vesselTypeName,asc')", example = "vesselTypeName,asc")
                @RequestParam(required = false) String sort) {

            log.info("Searching vessel types with page: {}, size: {}, sort: {}", page, size, sort);

            // Create Pageable
            Pageable pageable = createPageable(page, size, sort);

            // Document ID for Vessel Type is "100-003"
            String docId = "100-003";

            // Call service
            Map<String, Object> result = vesselTypeService.searchVesselTypes(docId, request, pageable);

            log.info("Successfully retrieved vessel types");
            return ApiResponse.success("Vessel types retrieved successfully", result);
        }

        @AllowedAction(UserRolesRightsEnum.VIEW)
        @GetMapping("/{id}")
        @Operation(
                summary = "Get vessel type details",
                description = "Retrieve complete vessel type information by ID",
                security = @SecurityRequirement(name = "bearerAuth")
        )
        @ApiResponses(value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Successfully retrieved vessel type",
                        content = @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = VesselTypeDto.class)
                        )
                ),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Vessel type not found",
                        content = @Content(mediaType = "application/json")
                ),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error",
                        content = @Content(mediaType = "application/json")
                )
        })
        public ResponseEntity<?> getVesselType(
                @Parameter(description = "Vessel Type POID", required = true, example = "12345")
                @PathVariable Long id) {
            log.info("Getting vessel type with id: {}", id);
            VesselTypeDto vesselType = vesselTypeService.getVesselType(id);
            log.info("Successfully retrieved vessel type with id: {}", id);
            return ApiResponse.success("Vessel type retrieved successfully", vesselType);
        }

        @AllowedAction(UserRolesRightsEnum.CREATE)
        @PostMapping
        @Operation(
                summary = "Create vessel type",
                description = "Create a new vessel type record",
                security = @SecurityRequirement(name = "bearerAuth")
        )
        @ApiResponses(value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Successfully created vessel type",
                        content = @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = VesselTypeDto.class)
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
        public ResponseEntity<?> createVesselType(
                @Parameter(description = "Vessel type creation data", required = true)
                @Valid @RequestBody VesselTypeCreateDTO dto) {
            log.info("Creating vessel type with code: {}, groupId: {}, userPoid: {}",
                    dto.getVesselTypeCode(), UserContext.getGroupPoid(), UserContext.getUserPoid());
            VesselTypeDto result = vesselTypeService.createVesselType(
                    dto,
                    UserContext.getGroupPoid(),
                    UserContext.getUserPoid()
            );
            log.info("Successfully created vessel type with id: {}", result.getVesselTypePoid());
            return ApiResponse.success("Vessel type created successfully", result);
        }

        @AllowedAction(UserRolesRightsEnum.EDIT)
        @PutMapping("/{id}")
        @Operation(
                summary = "Update vessel type",
                description = "Update an existing vessel type record",
                security = @SecurityRequirement(name = "bearerAuth")
        )
        @ApiResponses(value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Successfully updated vessel type",
                        content = @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = VesselTypeDto.class)
                        )
                ),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Vessel type not found",
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
        public ResponseEntity<?> updateVesselType(
                @Parameter(description = "Vessel Type POID", required = true, example = "12345")
                @PathVariable Long id,
                @Parameter(description = "Vessel type update data", required = true)
                @Valid @RequestBody VesselTypeUpdateDTO dto) {
            log.info("Updating vessel type with id: {}, groupId: {}, userPoid: {}",
                    id, UserContext.getGroupPoid(), UserContext.getUserPoid());
            VesselTypeDto result = vesselTypeService.updateVesselType(
                    id,
                    dto,
                    UserContext.getGroupPoid(),
                    UserContext.getUserPoid()
            );
            log.info("Successfully updated vessel type with id: {}", id);
            return ApiResponse.success("Vessel type updated successfully", result);
        }

        @AllowedAction(UserRolesRightsEnum.EDIT)
        @PatchMapping("/{id}/activate")
        @Operation(
                summary = "Toggle active status",
                description = "Toggle the active flag of a vessel type between Y and N",
                security = @SecurityRequirement(name = "bearerAuth")
        )
        @ApiResponses(value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Successfully toggled vessel type status",
                        content = @Content(mediaType = "application/json")
                ),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Vessel type not found",
                        content = @Content(mediaType = "application/json")
                ),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error",
                        content = @Content(mediaType = "application/json")
                )
        })
        public ResponseEntity<?> toggleActive(
                @Parameter(description = "Vessel Type POID", required = true, example = "12345")
                @PathVariable Long id) {
            log.info("Toggling active status for vessel type with id: {}", id);
            vesselTypeService.toggleActive(id);
            log.info("Successfully toggled active status for vessel type with id: {}", id);
            return ApiResponse.success("Vessel type status toggled successfully");
        }

        @AllowedAction(UserRolesRightsEnum.DELETE)
        @DeleteMapping("/{id}")
        @Operation(
                summary = "Delete vessel type",
                description = "Soft delete a vessel type by setting DELETED flag to Y and ACTIVE to N",
                security = @SecurityRequirement(name = "bearerAuth")
        )
        @ApiResponses(value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Successfully deleted vessel type",
                        content = @Content(mediaType = "application/json")
                ),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Vessel type not found",
                        content = @Content(mediaType = "application/json")
                ),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error",
                        content = @Content(mediaType = "application/json")
                )
        })
        public ResponseEntity<?> deleteVesselType(
                @Parameter(description = "Vessel Type POID", required = true, example = "12345")
                @PathVariable Long id) {
            log.info("Deleting vessel type with id: {}", id);
            vesselTypeService.deleteVesselType(id);
            log.info("Successfully deleted vessel type with id: {}", id);
            return ApiResponse.success("Vessel type deleted successfully");
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

                    // Map frontend field names to database column names
                    String dbColumn = mapSortFieldToColumn(sortField);

                    Sort.Direction direction = "DESC".equals(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
                    Sort sortObj = Sort.by(direction, dbColumn);
                    return PageRequest.of(page, size, sortObj);
                }
            }
            // Default sort by VESSEL_TYPE_NAME ascending
            Sort defaultSort = Sort.by(Sort.Direction.ASC, "VESSEL_TYPE_NAME");
            return PageRequest.of(page, size, defaultSort);
        }

        /**
         * Map frontend sort field names to database column names
         */
        private String mapSortFieldToColumn(String sortField) {
            if (sortField == null) {
                return "VESSEL_TYPE_NAME";
            }

            // Convert camelCase to UPPER_SNAKE_CASE
            String normalized = sortField.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();

            // Map common field names
            switch (normalized) {
                case "VESSEL_TYPE_POID":
                    return "VESSEL_TYPE_POID";
                case "VESSEL_TYPE_CODE":
                    return "VESSEL_TYPE_CODE";
                case "VESSEL_TYPE_NAME":
                    return "VESSEL_TYPE_NAME";
                case "VESSEL_TYPE_NAME2":
                    return "VESSEL_TYPE_NAME2";
                case "CREATED_DATE":
                    return "CREATED_DATE";
                case "LASTMODIFIED_DATE":
                    return "LASTMODIFIED_DATE";
                default:
                    return "VESSEL_TYPE_NAME"; // Default sort field
            }
        }
}
