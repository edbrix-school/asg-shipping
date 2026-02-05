package com.asg.shipping.agentMaster.controller;


import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.agentMaster.dto.ShipAgentMasterRequestDto;
import com.asg.shipping.agentMaster.dto.ShipAgentMasterResponseDto;
import com.asg.shipping.agentMaster.service.ShipAgentMasterService;
import com.asg.shipping.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/v1/agent-master")
@Validated
@Tag(
        name = "Agent Master",
        description = "APIs for managing Shipping Agent Master"
)
public class ShipAgentMasterController {

    private final ShipAgentMasterService shipAgentMasterService;
    private final LoggingService loggingService;

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createAgent(
            @Valid @RequestBody ShipAgentMasterRequestDto request
    ) {

        ShipAgentMasterResponseDto response =
                shipAgentMasterService.createAgentMaster(request);

        return ApiResponse.success("Agent Master created successfully", response
        );
    }


    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{agentPoid}")
    public ResponseEntity<?> updateAgent(
            @PathVariable @NotNull @Positive Long agentPoid,
            @Valid @RequestBody
            ShipAgentMasterRequestDto request
    ) {
        ShipAgentMasterResponseDto response =
                shipAgentMasterService.updateAgentMaster(agentPoid, request);

        return ApiResponse.success("Agent Master updated successfully", response
        );
    }


    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{agentPoid}")
    public ResponseEntity<?> getAgentById(
            @PathVariable @NotNull @Positive Long agentPoid
    ) {

        ShipAgentMasterResponseDto response =
                shipAgentMasterService.findByIdAgentMaster(agentPoid);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), agentPoid.toString());

        return ApiResponse.success("Agent Master retrieved successfully", response
        );
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{agentPoid}")
    public ResponseEntity<?> deleteAgent(
            @PathVariable @NotNull @Positive Long agentPoid
    ) {

        shipAgentMasterService.deleteAgentMaster(agentPoid);

        return ApiResponse.success("Agent Master deleted successfully");
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/search")
    @Operation(
            summary = "Search agents",
            description = "Retrieve paginated list of agents with optional filtering and sorting using DocumentSearchService",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved agents",
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
    public ResponseEntity<?> searchAgents(
            @RequestBody(required = false) com.asg.common.lib.dto.FilterRequestDto request,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field and direction (e.g., 'agentName,asc')", example = "agentName,asc")
            @RequestParam(required = false) String sort) {

        log.info("Searching agents with page: {}, size: {}, sort: {}", page, size, sort);

        // Create Pageable
        Pageable pageable = createPageable(page, size, sort);

        // Get document ID from UserContext (should be "100-063" for Agent Master)
        String docId = UserContext.getDocumentId();

        // Call service
        Map<String, Object> result = shipAgentMasterService.listAgents(docId, request, pageable);

        log.info("Successfully retrieved agents");
        return ApiResponse.success("Agents retrieved successfully", result);
    }

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
        // Default sort by AGENT_NAME ascending
        Sort defaultSort = Sort.by(Sort.Direction.ASC, "AGENT_NAME");
        return PageRequest.of(page, size, defaultSort);
    }

    private String mapSortFieldToColumn(String sortField) {
        if (sortField == null) {
            return "AGENT_NAME";
        }

        // Convert camelCase to UPPER_SNAKE_CASE
        String normalized = sortField.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();

        // Map common field names
        switch (normalized) {
            case "AGENT_POID":
                return "AGENT_POID";
            case "AGENT_NAME":
                return "AGENT_NAME";
            case "AGENT_NAME2":
                return "AGENT_NAME2";
            case "CONTACT_PERSON":
                return "CONTACT_PERSON";
            case "EMAIL":
                return "EMAIL";
            case "CONTACT_NO":
                return "CONTACT_NO";
            case "CREATED_DATE":
                return "CREATED_DATE";
            case "LASTMODIFIED_DATE":
                return "LASTMODIFIED_DATE";
            default:
                return "AGENT_NAME"; // Default sort field
        }
    }
}
