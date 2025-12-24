package com.asg.shipping.containertypes.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.common.ApiResponse;
import com.asg.shipping.containertypes.dto.ContainerTypeCreateDTO;
import com.asg.shipping.containertypes.dto.ContainerTypeDto;
import com.asg.shipping.containertypes.dto.ContainerTypeUpdateDTO;
import com.asg.shipping.containertypes.service.ContainerTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for Container Type operations
 */
@RestController
@RequestMapping("/v1/container-types")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Container Type Management", description = "APIs for managing container types")
public class ContainerTypeController {

    @Autowired
    ContainerTypeService containerTypeService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/search")
    @Operation(
            summary = "Search container types",
            description = "Retrieve paginated list of container types with optional filtering and sorting using DocumentSearchService"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved container types",
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
    public ResponseEntity<?> searchContainerTypes(
            @RequestBody(required = false) FilterRequestDto request,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field and direction (e.g., 'containerTypeName,asc')", example = "containerTypeName,asc")
            @RequestParam(required = false) String sort) {

        log.info("Searching container types with page: {}, size: {}, sort: {}", page, size, sort);

        // Create Pageable
        Pageable pageable = createPageable(page, size, sort);

        // Call service
        Map<String, Object> result = containerTypeService.searchContainerTypes(UserContext.getDocumentId(), request, pageable);

        log.info("Successfully retrieved container types");
        return ApiResponse.success("Container types retrieved successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{id}")
    @Operation(
            summary = "Get container type details",
            description = "Retrieve complete container type information by ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved container type",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ContainerTypeDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Container type not found",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> getContainerType(
            @Parameter(description = "Container Type POID", required = true, example = "12345")
            @PathVariable Long id) {
        log.info("Getting container type with id: {}", id);
        ContainerTypeDto containerType = containerTypeService.getContainerType(id);
        log.info("Successfully retrieved container type with id: {}", id);
        return ApiResponse.success("Container type retrieved successfully", containerType);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    @Operation(
            summary = "Create container type",
            description = "Create a new container type record"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully created container type",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ContainerTypeDto.class)
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
    public ResponseEntity<?> createContainerType(
            @Parameter(description = "Container type creation data", required = true)
            @Valid @RequestBody ContainerTypeCreateDTO dto) {
        log.info("Creating container type with code: {}, groupId: {}, userPoid: {}",
                dto.getContainerTypeCode(), UserContext.getGroupPoid(), UserContext.getUserPoid());
        ContainerTypeDto result = containerTypeService.createContainerType(
                dto,
                UserContext.getGroupPoid(),
                UserContext.getUserPoid()
        );
        log.info("Successfully created container type with id: {}", result.getContainerTypePoid());
        return ApiResponse.success("Container type created successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{id}")
    @Operation(
            summary = "Update container type",
            description = "Update an existing container type record"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully updated container type",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ContainerTypeDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Container type not found",
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
    public ResponseEntity<?> updateContainerType(
            @Parameter(description = "Container Type POID", required = true, example = "12345")
            @PathVariable Long id,
            @Parameter(description = "Container type update data", required = true)
            @Valid @RequestBody ContainerTypeUpdateDTO dto) {
        log.info("Updating container type with id: {}, groupId: {}, userPoid: {}",
                id, UserContext.getGroupPoid(), UserContext.getUserPoid());
        ContainerTypeDto result = containerTypeService.updateContainerType(
                id,
                dto,
                UserContext.getGroupPoid(),
                UserContext.getUserPoid()
        );
        log.info("Successfully updated container type with id: {}", id);
        return ApiResponse.success("Container type updated successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PatchMapping("/{id}/activate")
    @Operation(
            summary = "Toggle active status",
            description = "Toggle the active flag of a container type between Y and N"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully toggled container type status",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Container type not found",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> toggleActive(
            @Parameter(description = "Container Type POID", required = true, example = "12345")
            @PathVariable Long id) {
        log.info("Toggling active status for container type with id: {}", id);
        containerTypeService.toggleActive(id);
        log.info("Successfully toggled active status for container type with id: {}", id);
        return ApiResponse.success("Container type status toggled successfully");
    }

//    @AllowedAction(UserRolesRightsEnum.DELETE)
//    @DeleteMapping("/{id}")
//    @Operation(
//            summary = "Delete container type",
//            description = "Soft delete a container type by setting DELETED flag to Y and ACTIVE to N"
//    )
//    @ApiResponses(value = {
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(
//                    responseCode = "200",
//                    description = "Successfully deleted container type",
//                    content = @Content(mediaType = "application/json")
//            ),
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(
//                    responseCode = "404",
//                    description = "Container type not found",
//                    content = @Content(mediaType = "application/json")
//            ),
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(
//                    responseCode = "500",
//                    description = "Internal server error",
//                    content = @Content(mediaType = "application/json")
//            )
//    })
//    public ResponseEntity<?> deleteContainerType(
//            @Parameter(description = "Container Type POID", required = true, example = "12345")
//            @PathVariable Long id) {
//        log.info("Deleting container type with id: {}", id);
//        containerTypeService.deleteContainerType(id);
//        log.info("Successfully deleted container type with id: {}", id);
//        return ApiResponse.success("Container type deleted successfully");
//    }

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
        // Default sort by CONTAINER_TYPE_NAME ascending
        Sort defaultSort = Sort.by(Sort.Direction.ASC, "CONTAINER_TYPE_NAME");
        return PageRequest.of(page, size, defaultSort);
    }

    /**
     * Map frontend sort field names to database column names
     */
    private String mapSortFieldToColumn(String sortField) {
        if (sortField == null) {
            return "CONTAINER_TYPE_NAME";
        }

        // Convert camelCase to UPPER_SNAKE_CASE
        String normalized = sortField.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();

        // Map common field names
        switch (normalized) {
            case "CONTAINER_TYPE_POID":
                return "CONTAINER_TYPE_POID";
            case "CONTAINER_TYPE_CODE":
                return "CONTAINER_TYPE_CODE";
            case "CONTAINER_TYPE_NAME":
                return "CONTAINER_TYPE_NAME";
            case "CONTAINER_TYPE_SIZE":
                return "CONTAINER_TYPE_SIZE";
            case "CONTAINER_TYPE_ISO_NAME":
                return "CONTAINER_TYPE_ISO_NAME";
            case "CONTAINER_TYPE_CATEGORY":
                return "CONTAINER_TYPE_CATEGORY";
            case "CREATED_DATE":
                return "CREATED_DATE";
            case "LASTMODIFIED_DATE":
                return "LASTMODIFIED_DATE";
            default:
                return "CONTAINER_TYPE_NAME"; // Default sort field
        }
    }
}

