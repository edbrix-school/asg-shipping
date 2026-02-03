package com.asg.shipping.terminal.containertype.controller;

import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.shipping.terminal.containertype.dto.ContainerTerminalTypeRequest;
import com.asg.shipping.terminal.containertype.dto.ContainerTerminalTypeResponse;
import com.asg.shipping.terminal.containertype.service.ContainerTerminalTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import static com.asg.shipping.common.ApiResponse.internalServerError;
import static com.asg.shipping.common.ApiResponse.success;

@RestController
@RequestMapping("/v1/container-terminal-types")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ContainerTerminalTypeController {

    private final ContainerTerminalTypeService containerTerminalTypeService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "List Container Terminal Types with Search and Sort",
            description = """
                    Provide search filters.
                    Valid `searchField` values:
                    - GLOBALSEARCH
                    - CONTAINER_TMNL_TYPE_CODE
                    - CONTAINER_TMNL_TYPE_NAME

                    Sorting will be applied as specified in list_of_records_sql
                    configured in doc_master table.
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
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
                                    name = "Container Terminal Type Filters",
                                    value = """
                                            {
                                              "operator": "OR",
                                              "isDeleted": "N",
                                              "filters": [
                                                { "searchField": "GLOBALSEARCH", "searchValue": "20" },
                                                { "searchField": "CONTAINER_TMNL_TYPE_CODE", "searchValue": "ISO20" },
                                                { "searchField": "CONTAINER_TMNL_TYPE_NAME", "searchValue": "Twenty Feet" }
                                              ]
                                            }
                                            """
                            )
                    }
            )
    )
    @PostMapping("/list")
    public ResponseEntity<?> listContainerTerminalTypes(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters,
            @RequestHeader("X-Document-Id") String docId) {

        try {
            log.info("List ContainerTerminalTypes request | page={}, size={}, docId={}",
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    docId);
            Map<String, Object> result =
                    containerTerminalTypeService.listContainerTerminalTypes(
                            docId,
                            filters,
                            pageable
                    );
            return success("Container Terminal Type list fetched successfully", result);
        } catch (Exception e) {
            return internalServerError("Unable to fetch container terminal types: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{containerTerminalTypePoid}")
    @Operation(
            summary = "Get Container Terminal Type by ID",
            description = "Retrieve container terminal type details using POID and Group POID",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getById(
            @PathVariable @NotNull @Positive Long containerTerminalTypePoid,
            @RequestHeader("X-Group-Poid") Long groupPoid
    ) {
        log.info("Get ContainerTerminalType request | poid={}, groupPoid={}",
                containerTerminalTypePoid,
                groupPoid);
        ContainerTerminalTypeResponse response =
                containerTerminalTypeService.getById(containerTerminalTypePoid, groupPoid);

        return success(
                "Container terminal type retrieved successfully",
                response
        );
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    @Operation(
            summary = "Create Container Terminal Type",
            description = "Creates a new container terminal type",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> create(
            @Valid @RequestBody ContainerTerminalTypeRequest request,
            @RequestHeader("X-Group-Poid") Long groupPoid,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Document-Id") String docId
    ) {
        log.info("Create ContainerTerminalType request | code={}, groupPoid={}, userId={}",
                request.getContainerTerminalTypeCode(),
                groupPoid,
                userId);
        ContainerTerminalTypeResponse response =
                containerTerminalTypeService.create(request, groupPoid, userId, docId);

        return success(
                "Container terminal type created successfully",
                response
        );
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{containerTerminalTypePoid}")
    @Operation(
            summary = "Update Container Terminal Type",
            description = "Updates an existing container terminal type",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> update(
            @PathVariable @NotNull @Positive Long containerTerminalTypePoid,
            @Valid @RequestBody ContainerTerminalTypeRequest request,
            @RequestHeader("X-Group-Poid") Long groupPoid,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Document-Id") String docId
    ) {
        log.info("Update ContainerTerminalType request | poid={}, code={}, groupPoid={}, userId={}",
                containerTerminalTypePoid,
                request.getContainerTerminalTypeCode(),
                groupPoid,
                userId);

        ContainerTerminalTypeResponse response =
                containerTerminalTypeService.update(containerTerminalTypePoid, request, groupPoid, userId, docId);

        return success(
                "Container terminal type updated successfully",
                response
        );
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{containerTerminalTypePoid}/activate")
    @Operation(
            summary = "Toggle Active Status",
            description = "Toggles the active status of a container terminal type between Y and N",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> toggleActiveStatus(
            @PathVariable @NotNull @Positive Long containerTerminalTypePoid) {

        Long groupPoid = UserContext.getGroupPoid();
        String userId = UserContext.getUserId();

        log.info("Toggle Active Status request | poid={}, groupPoid={}, userId={}",
                containerTerminalTypePoid,
                groupPoid,
                userId);

        containerTerminalTypeService.toggleActiveStatus(containerTerminalTypePoid, groupPoid, userId);

        log.info("Toggle Active Status completed successfully | poid={}", containerTerminalTypePoid);

        return success("Container terminal type status toggled successfully");
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{containerTerminalTypePoid}")
    @Operation(
            summary = "Delete Container Terminal Type",
            description = "Deletes a container terminal type",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> delete(
            @PathVariable @NotNull @Positive Long containerTerminalTypePoid,
            @RequestHeader("X-Group-Poid") Long groupPoid,
            @RequestHeader("X-User-Id") String userId
    ) {
        log.info("Delete ContainerTerminalType request | poid={}, groupPoid={}, userId={}",
                containerTerminalTypePoid,
                groupPoid,
                userId);
        containerTerminalTypeService.delete(containerTerminalTypePoid, groupPoid, userId);
        return success("Container terminal type deleted successfully");
    }
}

