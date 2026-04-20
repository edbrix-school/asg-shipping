package com.asg.shipping.linemasterthirdparty.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.response.ApiResponse;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.linemasterthirdparty.dto.*;
import com.asg.shipping.linemasterthirdparty.service.LineMasterThirdPartyService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for Line Master Third Party operations
 */
@RestController
@RequestMapping("/v1/line-master-third-party")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Line Master Third Party Management", description = "APIs for managing third party shipping lines")
public class LineMasterThirdPartyController {

    private final LineMasterThirdPartyService lineService;
    private final LoggingService loggingService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/search")
    @Operation(
            summary = "Search third party lines",
            description = "Retrieve paginated list of third party lines with optional filtering and sorting using DocumentSearchService. All results are filtered by LINE_TYPE = 'THIRD_PARTY'.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved third party lines",
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
    public ResponseEntity<?> searchThirdPartyLines(
            @RequestBody(required = false) FilterRequestDto request,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field and direction (e.g., 'lineName,asc')", example = "lineName,asc")
            @RequestParam(required = false) String sort) {

        log.info("Searching third party lines with page: {}, size: {}, sort: {}", page, size, sort);

        Pageable pageable = createPageable(page, size, sort);
        Map<String, Object> result = lineService.searchThirdPartyLines(request, pageable);

        log.info("Successfully retrieved third party lines");
        return ApiResponse.success("Third party lines retrieved successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{id}")
    @Operation(
            summary = "Get third party line details",
            description = "Retrieve complete third party line information by ID. Validates that LINE_TYPE = 'THIRD_PARTY'.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved third party line",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LineMasterThirdPartyDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Third party line not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> getThirdPartyLine(
            @Parameter(description = "Line POID", required = true, example = "12345")
            @PathVariable Long id) {
        log.info("Getting third party line with id: {}", id);
        LineMasterThirdPartyDto line = lineService.getThirdPartyLine(id);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), id.toString());
        log.info("Successfully retrieved third party line with id: {}", id);
        return ApiResponse.success("Third party line retrieved successfully", line);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    @Operation(
            summary = "Create third party line",
            description = "Create a new third party line record. LINE_TYPE is automatically set to 'THIRD_PARTY'.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully created third party line",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LineMasterThirdPartyDto.class)
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
    public ResponseEntity<?> createThirdPartyLine(
            @Parameter(description = "Third party line creation data", required = true)
            @Valid @RequestBody LineMasterThirdPartyCreateDTO dto) {
        log.info("Creating third party line with code: {}, name: {}", dto.getLineCode(), dto.getLineName());
        LineMasterThirdPartyDto result = lineService.createThirdPartyLine(dto);
        log.info("Successfully created third party line with id: {}", result.getLinePoid());
        return ApiResponse.success("Third party line created successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{id}")
    @Operation(
            summary = "Update third party line",
            description = "Update an existing third party line record. Validates that LINE_TYPE = 'THIRD_PARTY' and ensures it remains 'THIRD_PARTY'.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully updated third party line",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LineMasterThirdPartyDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Third party line not found",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input parameters or validation error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> updateThirdPartyLine(
            @Parameter(description = "Line POID", required = true, example = "12345")
            @PathVariable Long id,
            @Parameter(description = "Third party line update data", required = true)
            @Valid @RequestBody LineMasterThirdPartyUpdateDTO dto) {
        log.info("Updating third party line with id: {}", id);
        LineMasterThirdPartyDto result = lineService.updateThirdPartyLine(id, dto);
        log.info("Successfully updated third party line with id: {}", id);
        return ApiResponse.success("Third party line updated successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{id}/activate")
    @Operation(
            summary = "Toggle active status",
            description = "Toggle the active flag of a third party line between Y and N. Validates that LINE_TYPE = 'THIRD_PARTY'.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully toggled third party line status",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Third party line not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> toggleActive(
            @Parameter(description = "Line POID", required = true, example = "12345")
            @PathVariable Long id) {
        log.info("Toggling active status for third party line with id: {}", id);
        lineService.toggleActive(id);
        log.info("Successfully toggled active status for third party line with id: {}", id);
        return ApiResponse.success("Third party line status toggled successfully");
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete third party line",
            description = "Soft delete a third party line by setting DELETED flag to Y and ACTIVE to N. Validates that LINE_TYPE = 'THIRD_PARTY'.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully deleted third party line",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Third party line not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> deleteThirdPartyLine(
            @Parameter(description = "Line POID", required = true, example = "12345")
            @PathVariable Long id,
            @Valid @RequestBody(required = false) com.asg.common.lib.dto.DeleteReasonDto deleteReasonDto) {
        log.info("Deleting third party line with id: {}", id);
        lineService.deleteThirdPartyLine(id, deleteReasonDto);
        log.info("Successfully deleted third party line with id: {}", id);
        return ApiResponse.success("Third party line deleted successfully");
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
            case "CREATED_DATE":
                return "CREATED_DATE";
            case "LASTMODIFIED_DATE":
                return "LASTMODIFIED_DATE";
            default:
                return "LINE_NAME";
        }
    }
}

