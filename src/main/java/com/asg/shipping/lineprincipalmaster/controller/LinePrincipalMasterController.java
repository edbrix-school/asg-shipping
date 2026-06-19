package com.asg.shipping.lineprincipalmaster.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.common.ApiResponse;
import com.asg.shipping.lineprincipalmaster.dto.*;
import com.asg.shipping.lineprincipalmaster.service.LinePrincipalMasterService;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * REST Controller for Line Principal Master operations
 */
@RestController
@RequestMapping("/v1/line-principal-master")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Line Principal Master Management", description = "APIs for managing shipping lines and principals with charges")
public class LinePrincipalMasterController {

    private final LinePrincipalMasterService lineService;
        private final LoggingService loggingService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    @Operation(
            summary = "Search lines",
            description = "Retrieve paginated list of lines with optional filtering and sorting using DocumentSearchService",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
        @Parameter(
                name = "X-Document-Id",
                in = ParameterIn.HEADER,
                description = "Document identifier for auditing",
                example = "100-007",
                required = true,
                schema = @Schema(type = "string", example = "100-007")
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
                    description = "Successfully retrieved lines",
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
    public ResponseEntity<?> searchLines(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        Map<String, Object> result = lineService.searchLines(UserContext.getDocumentId(), filters, startDate, endDate, pageable);

        log.info("Successfully retrieved lines");
        return ApiResponse.success("Lines retrieved successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{id}")
    @Operation(
            summary = "Get line details",
            description = "Retrieve complete line information by ID including charges",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
        @Parameter(
                name = "X-Document-Id",
                in = ParameterIn.HEADER,
                description = "Document identifier for auditing",
                example = "100-007",
                required = true,
                schema = @Schema(type = "string", example = "100-007")
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
                    description = "Successfully retrieved line",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LinePrincipalMasterDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Line not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> getLine(
            @Parameter(description = "Line POID", required = true, example = "12345")
            @PathVariable Long id) {
        log.info("Getting line with id: {}", id);
        LinePrincipalMasterDto line = lineService.getLine(id);
                loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), id.toString());
        log.info("Successfully retrieved line with id: {}", id);
        return ApiResponse.success("Line retrieved successfully", line);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    @Operation(
            summary = "Create line",
            description = "Create a new line record with optional charges",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
        @Parameter(
                name = "X-Document-Id",
                in = ParameterIn.HEADER,
                description = "Document identifier for auditing",
                example = "100-007",
                required = true,
                schema = @Schema(type = "string", example = "100-007")
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
                    description = "Successfully created line",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LinePrincipalMasterDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input parameters or validation error",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Line code or name already exists",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> createLine(
            @Parameter(description = "Line creation data", required = true)
            @Valid @RequestBody LinePrincipalMasterCreateDTO dto) {
        log.info("Creating line with code: {}, name: {}", dto.getLineCode(), dto.getLineName());
        LinePrincipalMasterDto result = lineService.createLine(dto);
        log.info("Successfully created line with id: {}", result.getLinePoid());
        return ApiResponse.success("Line created successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{id}")
    @Operation(
            summary = "Update line",
            description = "Update an existing line record and its charges",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
        @Parameter(
                name = "X-Document-Id",
                in = ParameterIn.HEADER,
                description = "Document identifier for auditing",
                example = "100-007",
                required = true,
                schema = @Schema(type = "string", example = "100-007")
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
                    description = "Successfully updated line",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LinePrincipalMasterDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Line not found",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input parameters or validation error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> updateLine(
            @Parameter(description = "Line POID", required = true, example = "12345")
            @PathVariable Long id,
            @Parameter(description = "Line update data", required = true)
            @Valid @RequestBody LinePrincipalMasterUpdateDTO dto) {
        log.info("Updating line with id: {}", id);
        LinePrincipalMasterDto result = lineService.updateLine(id, dto);
        log.info("Successfully updated line with id: {}", id);
        return ApiResponse.success("Line updated successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{id}/activate")
    @Operation(
            summary = "Toggle active status",
            description = "Toggle the active flag of a line between Y and N",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
        @Parameter(
                name = "X-Document-Id",
                in = ParameterIn.HEADER,
                description = "Document identifier for auditing",
                example = "100-007",
                required = true,
                schema = @Schema(type = "string", example = "100-007")
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
                    description = "Successfully toggled line status",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Line not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> toggleActive(
            @Parameter(description = "Line POID", required = true, example = "12345")
            @PathVariable Long id) {
        log.info("Toggling active status for line with id: {}", id);
        lineService.toggleActive(id);
        log.info("Successfully toggled active status for line with id: {}", id);
        return ApiResponse.success("Line status toggled successfully");
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete line",
            description = "Soft delete a line by setting DELETED flag to Y and ACTIVE to N",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
        @Parameter(
                name = "X-Document-Id",
                in = ParameterIn.HEADER,
                description = "Document identifier for auditing",
                example = "100-007",
                required = true,
                schema = @Schema(type = "string", example = "100-007")
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
                    description = "Successfully deleted line",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Line not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> deleteLine(
            @Parameter(description = "Line POID", required = true, example = "12345")
                        @PathVariable Long id,
                        @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {
        log.info("Deleting line with id: {}", id);
                lineService.deleteLine(id, deleteReasonDto);
        log.info("Successfully deleted line with id: {}", id);
        return ApiResponse.success("Line deleted successfully");
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/{id}/copy-charges")
    @Operation(
            summary = "Copy charges from another line",
            description = "Copy all charges from a source line to the target line",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
        @Parameter(
                name = "X-Document-Id",
                in = ParameterIn.HEADER,
                description = "Document identifier for auditing",
                example = "100-007",
                required = true,
                schema = @Schema(type = "string", example = "100-007")
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
                    description = "Successfully copied charges",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Line not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> copyCharges(
            @Parameter(description = "Target Line POID", required = true, example = "12345")
            @PathVariable Long id,
            @Parameter(description = "Copy charges request", required = true)
            @Valid @RequestBody CopyChargesRequestDto request) {
        log.info("Copying charges from line {} to line {}", request.getSourceLinePoid(), id);
        CopyChargesRequestDto result = lineService.copyCharges(id, request);
        log.info("Successfully copied charges to line {}", id);
        return ApiResponse.success("Charges copied successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/{id}/create-gl")
    @Operation(
            summary = "Create GL master",
            description = "Create GL master and sub accounts for a line",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
        @Parameter(
                name = "X-Document-Id",
                in = ParameterIn.HEADER,
                description = "Document identifier for auditing",
                example = "100-007",
                required = true,
                schema = @Schema(type = "string", example = "100-007")
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
                    description = "Successfully created GL master",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Line not found",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Error creating GL master",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> createGlMaster(
            @Parameter(description = "Line POID", required = true, example = "12345")
            @PathVariable Long id) {
        log.info("Creating GL master for line with id: {}", id);
        lineService.createGlMaster(id);
        log.info("Successfully created GL master for line with id: {}", id);
        return ApiResponse.success("Line GL master and Sub accounts created successfully");
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
        Sort defaultSort = Sort.by(Sort.Direction.ASC, "LINE_NAME");
        return PageRequest.of(page, size, defaultSort);
    }

    /**
     * Map frontend sort field names to database column names
     */
    private String mapSortFieldToColumn(String sortField) {
        if (sortField == null) {
            return "LINE_NAME";
        }
        
        String normalized = sortField.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
        
        switch (normalized) {
            case "LINE_POID":
                return "LINE_POID";
            case "LINE_CODE":
                return "LINE_CODE";
            case "LINE_NAME":
                return "LINE_NAME";
            case "LINE_NAME2":
                return "LINE_NAME2";
            case "LINE_TYPE":
                return "LINE_TYPE";
            case "CREATED_DATE":
                return "CREATED_DATE";
            case "LASTMODIFIED_DATE":
                return "LASTMODIFIED_DATE";
            default:
                return "LINE_NAME";
        }
    }
}

