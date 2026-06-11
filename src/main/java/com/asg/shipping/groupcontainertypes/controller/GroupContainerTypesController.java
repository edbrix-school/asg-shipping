package com.asg.shipping.groupcontainertypes.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.common.ApiResponse;
import com.asg.shipping.groupcontainertypes.dto.ContainerGroupCreateDTO;
import com.asg.shipping.groupcontainertypes.dto.ContainerGroupDto;
import com.asg.shipping.groupcontainertypes.dto.ContainerGroupUpdateDTO;
import com.asg.shipping.groupcontainertypes.service.GroupContainerTypesService;
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
 * REST Controller for Group Container Types operations
 */
@RestController
@RequestMapping("/v1/group-container-types")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Group Container Types Management", description = "APIs for managing group container types")
public class GroupContainerTypesController {

    @Autowired
    GroupContainerTypesService containerGroupService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/search")
    @Operation(
            summary = "Search container groups",
            description = "Retrieve paginated list of container groups with optional filtering and sorting using DocumentSearchService"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved container groups",
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
    public ResponseEntity<?> searchContainerGroups(
            @RequestBody(required = false) FilterRequestDto request,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field and direction (e.g., 'containerGrpName,asc')", example = "containerGrpName,asc")
            @RequestParam(required = false) String sort) {

        log.info("Searching container groups with page: {}, size: {}, sort: {}", page, size, sort);

        // Create Pageable
        Pageable pageable = createPageable(page, size, sort);

        // Call service
        Map<String, Object> result = containerGroupService.searchContainerGroups(UserContext.getDocumentId(), request, pageable);

        log.info("Successfully retrieved container groups");
        return ApiResponse.success("Container groups retrieved successfully", result);
    }


    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{id}")
    @Operation(
            summary = "Get container group details",
            description = "Retrieve complete container group information by ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved container group",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ContainerGroupDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Container group not found",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> getContainerGroup(
            @Parameter(description = "Container Group POID", required = true, example = "12345")
            @PathVariable Long id) {
        log.info("Getting container group with id: {}", id);
        ContainerGroupDto containerGroup = containerGroupService.getContainerGroup(id);
        log.info("Successfully retrieved container group with id: {}", id);
        return ApiResponse.success("Container group retrieved successfully", containerGroup);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    @Operation(
            summary = "Create container group",
            description = "Create a new container group record"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully created container group",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ContainerGroupDto.class)
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
    public ResponseEntity<?> createContainerGroup(
            @Parameter(description = "Container group creation data", required = true)
            @Valid @RequestBody ContainerGroupCreateDTO dto) {
        log.info("Creating container group with code: {}, groupId: {}, userPoid: {}",
                dto.getContainerGrpCode(), UserContext.getGroupPoid(), UserContext.getUserPoid());
        ContainerGroupDto result = containerGroupService.createContainerGroup(
                dto,
                UserContext.getGroupPoid(),
                UserContext.getUserPoid()
        );
        log.info("Successfully created container group with id: {}", result.getContainerGrpPoid());
        return ApiResponse.success("Container group created successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{id}")
    @Operation(
            summary = "Update container group",
            description = "Update an existing container group record"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully updated container group",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ContainerGroupDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Container group not found",
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
    public ResponseEntity<?> updateContainerGroup(
            @Parameter(description = "Container Group POID", required = true, example = "12345")
            @PathVariable Long id,
            @Parameter(description = "Container group update data", required = true)
            @Valid @RequestBody ContainerGroupUpdateDTO dto) {
        log.info("Updating container group with id: {}, groupId: {}, userPoid: {}",
                id, UserContext.getGroupPoid(), UserContext.getUserPoid());
        ContainerGroupDto result = containerGroupService.updateContainerGroup(
                id,
                dto,
                UserContext.getGroupPoid(),
                UserContext.getUserPoid()
        );
        log.info("Successfully updated container group with id: {}", id);
        return ApiResponse.success("Container group updated successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PatchMapping("/{id}/activate")
    @Operation(
            summary = "Toggle active status",
            description = "Toggle the active flag of a container group between Y and N"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully toggled container group status",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Container group not found",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> toggleActive(
            @Parameter(description = "Container Group POID", required = true, example = "12345")
            @PathVariable Long id) {
        log.info("Toggling active status for container group with id: {}", id);
        containerGroupService.toggleActive(id);
        log.info("Successfully toggled active status for container group with id: {}", id);
        return ApiResponse.success("Container group status toggled successfully");
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete container group",
            description = "Soft delete a container group using DocumentDeleteService"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully deleted container group",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Container group not found",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> deleteContainerGroup(
            @Parameter(description = "Container Group POID", required = true, example = "12345")
            @PathVariable Long id,
            @Parameter(description = "Delete reason information", required = true)
            @Valid @RequestBody DeleteReasonDto deleteReasonDto) {
        log.info("Deleting container group with id: {}", id);
        containerGroupService.deleteContainerGroup(id, deleteReasonDto);
        log.info("Successfully deleted container group with id: {}", id);
        return ApiResponse.success("Container group deleted successfully");
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
        // Default sort by CONTAINER_GRP_NAME ascending
        Sort defaultSort = Sort.by(Sort.Direction.ASC, "CONTAINER_GRP_NAME");
        return PageRequest.of(page, size, defaultSort);
    }


    /**
     * Map frontend sort field names to database column names
     */
    private String mapSortFieldToColumn(String sortField) {
        if (sortField == null) {
            return "CONTAINER_GRP_NAME";
        }

        // Convert camelCase to UPPER_SNAKE_CASE
        String normalized = sortField.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();

        // Map common field names
        switch (normalized) {
            case "CONTAINER_GRP_POID":
                return "CONTAINER_GRP_POID";
            case "CONTAINER_GRP_CODE":
                return "CONTAINER_GRP_CODE";
            case "CONTAINER_GRP_NAME":
                return "CONTAINER_GRP_NAME";
            case "CREATED_DATE":
                return "CREATED_DATE";
            case "LASTMODIFIED_DATE":
                return "LASTMODIFIED_DATE";
            default:
                return "CONTAINER_GRP_NAME"; // Default sort field
        }
    }

}
