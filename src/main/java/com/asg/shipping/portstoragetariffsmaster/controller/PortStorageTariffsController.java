package com.asg.shipping.portstoragetariffsmaster.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.common.ApiResponse;
import com.asg.shipping.portstoragetariffsmaster.dto.PortStorageTariffCreateDTO;
import com.asg.shipping.portstoragetariffsmaster.dto.PortStorageTariffDto;
import com.asg.shipping.portstoragetariffsmaster.dto.PortStorageTariffUpdateDTO;
import com.asg.shipping.portstoragetariffsmaster.service.PortStorageTariffsService;
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

import java.time.LocalDate;
import java.util.Map;

import static com.asg.common.lib.security.util.UserContext.getCompanyPoid;
import static com.asg.common.lib.security.util.UserContext.getGroupPoid;
import static com.asg.common.lib.security.util.UserContext.getUserPoid;

/**
 * REST Controller for Port Storage Tariffs Master operations
 */
@RestController
@RequestMapping("/v1/port-storage-tariffs-master")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Port Storage Tariffs Master Management", description = "APIs for managing port storage tariffs with storage slabs")
public class PortStorageTariffsController {

    private static final String DOC_ID = "100-060";

    private final PortStorageTariffsService tariffService;
    private final LoggingService loggingService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/search")
    @Operation(
            summary = "Search port storage tariffs",
            description = "Retrieve paginated list of port storage tariffs with optional filtering and sorting. "
                    + "When periodFrom and periodTo are provided, returns tariffs whose PERIOD_TO falls within that window.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved tariffs",
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
    public ResponseEntity<?> searchTariffs(
            @RequestBody(required = false) com.asg.common.lib.dto.FilterRequestDto request,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field and direction (e.g., 'PERIOD_FROM,desc')", example = "PERIOD_FROM,desc")
            @RequestParam(required = false) String sort,
            @Parameter(description = "Period window start (inclusive). Returns tariffs whose PERIOD_TO falls in this range.")
            @RequestParam(required = false) LocalDate periodFrom,
            @Parameter(description = "Period window end (inclusive). Returns tariffs whose PERIOD_TO falls in this range.")
            @RequestParam(required = false) LocalDate periodTo) {

        log.info("Searching port storage tariffs with page: {}, size: {}, sort: {}, periodFrom: {}, periodTo: {}",
                page, size, sort, periodFrom, periodTo);

        if ((periodFrom == null && periodTo != null) || (periodFrom != null && periodTo == null)) {
            return ApiResponse.badRequest("Both periodFrom and periodTo should be specified or both dates should be empty.");
        }
        if (periodFrom != null && periodFrom.isAfter(periodTo)) {
            return ApiResponse.badRequest("periodFrom must be less than or equal to periodTo.");
        }

        Pageable pageable = createPageable(page, size, sort);
        Map<String, Object> result = tariffService.searchTariffs(DOC_ID, request, periodFrom, periodTo, pageable);

        log.info("Successfully retrieved port storage tariffs");
        return ApiResponse.success("Tariffs retrieved successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{id}")
    @Operation(
            summary = "Get tariff details",
            description = "Retrieve complete tariff information by ID including storage slab details",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved tariff",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PortStorageTariffDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Tariff not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> getTariff(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long id) {

        log.info("Getting tariff with id: {}", id);
        PortStorageTariffDto tariff = tariffService.getTariff(id);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), id.toString());
        log.info("Successfully retrieved tariff with id: {}", id);
        return ApiResponse.success("Tariff retrieved successfully", tariff);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    @Operation(
            summary = "Create new tariff",
            description = "Create a new port storage tariff with optional storage slab details",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Successfully created tariff",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PortStorageTariffDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or validation error",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Conflict - Document reference already exists or period overlaps",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> createTariff(
            @Valid @RequestBody PortStorageTariffCreateDTO dto) {

        log.info("Creating tariff with description: {}", dto.getDescription());

        Long groupPoid = getGroupPoid();
        Long userPoid = getUserPoid();
        Long companyPoid = getCompanyPoid();

        PortStorageTariffDto created = tariffService.createTariff(dto, groupPoid, userPoid, companyPoid);

        log.info("Successfully created tariff with id: {}", created.getTransactionPoid());
        return ApiResponse.success("Tariff created successfully", created);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{id}")
    @Operation(
            summary = "Update tariff",
            description = "Update an existing port storage tariff and manage storage slab details",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully updated tariff",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PortStorageTariffDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or validation error",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Tariff not found",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Conflict - Document reference already exists or period overlaps",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> updateTariff(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long id,
            @Valid @RequestBody PortStorageTariffUpdateDTO dto) {

        log.info("Updating tariff with id: {}", id);

        Long groupPoid = getGroupPoid();
        Long userPoid = getUserPoid();
        Long companyPoid = getCompanyPoid();

        PortStorageTariffDto updated = tariffService.updateTariff(id, dto, groupPoid, userPoid, companyPoid);

        log.info("Successfully updated tariff with id: {}", id);
        return ApiResponse.success("Tariff updated successfully", updated);
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete tariff",
            description = "Deletes a port storage tariff (soft delete). Checks dependencies before deletion.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully deleted tariff"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Tariff not found",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Cannot delete due to dependencies",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> deleteTariff(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long id,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {

        log.info("deleteTariff started for companyPoid={} groupPoid={}", 
            getCompanyPoid(), getGroupPoid());
        
        tariffService.deleteTariff(getGroupPoid(), id, getCompanyPoid(), deleteReasonDto);
        
        log.info("deleteTariff completed for companyPoid={} groupPoid={}", 
            getCompanyPoid(), getGroupPoid());
        
        return ApiResponse.success("Tariff deleted successfully", null);
    }

    /**
     * Create Pageable from request parameters
     */
    private Pageable createPageable(int page, int size, String sort) {
        Sort sortObj = Sort.unsorted();
        if (sort != null && !sort.isEmpty()) {
            String[] sortParts = sort.split(",");
            if (sortParts.length == 2) {
                Sort.Direction direction = sortParts[1].trim().equalsIgnoreCase("desc")
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;
                sortObj = Sort.by(direction, sortParts[0].trim());
            } else if (sortParts.length == 1) {
                sortObj = Sort.by(Sort.Direction.ASC, sortParts[0].trim());
            }
        }
        return PageRequest.of(page, size, sortObj);
    }
}