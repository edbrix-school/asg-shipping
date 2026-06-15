package com.asg.shipping.linetariffs.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.shipping.common.ApiResponse;
import com.asg.shipping.linetariffs.dto.CopyTariffRequestDTO;
import com.asg.shipping.linetariffs.dto.LineTariffCreateDTO;
import com.asg.shipping.linetariffs.dto.LineTariffDto;
import com.asg.shipping.linetariffs.dto.LineTariffUpdateDTO;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.linetariffs.service.LineTariffsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.asg.common.lib.security.util.UserContext.getGroupPoid;
import static com.asg.common.lib.security.util.UserContext.getUserPoid;

/**
 * REST Controller for Line Tariffs (Demurrage and Detention Slabs) operations
 */
@RestController
@RequestMapping("/v1/line-tariffs-demurrage-detention")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Line Tariffs Management", description = "APIs for managing line tariffs with demurrage and detention slabs")
public class LineTariffsController {

    private static final String DOC_ID = "100-050";

    private final LineTariffsService lineTariffsService;
    private final LoggingService loggingService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/search")
    @Operation(
            summary = "Search line tariffs",
            description = "Retrieve paginated line tariffs with optional filtering and sorting. Use isDeleted=Y to view deleted records.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved line tariffs",
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
    public ResponseEntity<?> searchLineTariffs(
            @RequestBody(required = false) com.asg.common.lib.dto.FilterRequestDto request,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field and direction (e.g., 'description,asc')", example = "description,asc")
            @RequestParam(required = false) String sort) {

        log.info("Searching line tariffs with page: {}, size: {}, sort: {}", page, size, sort);

        try {
            Pageable pageable = createPageable(page, size, sort);
            Map<String, Object> result = lineTariffsService.searchLineTariffs(DOC_ID, request, pageable, null, null);

            log.info("Successfully retrieved line tariffs");
            return ApiResponse.success("Line tariffs retrieved successfully", result);
        } catch (Exception e) {
            return ApiResponse.internalServerError("Unable to fetch line tariffs: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{id}")
    @Operation(
            summary = "Get line tariff details",
            description = "Retrieve complete line tariff information by ID including all four detail tables and LOV data",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved line tariff",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LineTariffDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Line tariff not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> getLineTariff(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long id) {

        log.info("Getting line tariff with id: {}", id);
        LineTariffDto tariff = lineTariffsService.getLineTariff(id);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), id.toString());
        log.info("Successfully retrieved line tariff with id: {}", id);
        return ApiResponse.success("Line tariff retrieved successfully", tariff);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    @Operation(
            summary = "Create new line tariff",
            description = "Create a new line tariff with nested detail records for all four detail tables",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Successfully created line tariff",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LineTariffDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or validation error",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Conflict - Period overlaps or document reference already exists",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> createLineTariff(
            @Valid @RequestBody LineTariffCreateDTO dto) {

        log.info("Creating line tariff for line: {}, period: {} to {}", dto.getLinePoid(), dto.getPeriodFrom(), dto.getPeriodTo());

        Long groupPoid = getGroupPoid();
        Long userPoid = getUserPoid();

        LineTariffDto created = lineTariffsService.createLineTariff(dto, groupPoid, userPoid);

        log.info("Successfully created line tariff with id: {}", created.getTransactionPoid());
        return ApiResponse.success("Line tariff created successfully", created);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{id}")
    @Operation(
            summary = "Update line tariff",
            description = "Update an existing line tariff and manage nested detail records",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully updated line tariff",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LineTariffDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or validation error",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Line tariff not found",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Conflict - Period overlaps or document reference already exists",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> updateLineTariff(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long id,
            @Valid @RequestBody LineTariffUpdateDTO dto) {

        log.info("Updating line tariff with id: {}", id);

        Long groupPoid = getGroupPoid();
        Long userPoid = getUserPoid();

        LineTariffDto updated = lineTariffsService.updateLineTariff(id, dto, groupPoid, userPoid);

        log.info("Successfully updated line tariff with id: {}", id);
        return ApiResponse.success("Line tariff updated successfully", updated);
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete line tariff",
            description = "Soft delete a line tariff using document delete service",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully deleted line tariff"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Line tariff not found",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> deleteLineTariff(
            @PathVariable @NotNull @Min(1) Long id,
            @RequestBody(required = false) DeleteReasonDto deleteReasonDto
    ) {
        try {
            lineTariffsService.deleteLineTariff(id, deleteReasonDto);
            return ApiResponse.success("Line tariff deleted successfully", null);
        } catch (ResourceNotFoundException e) {
            log.error("Error deleting line tariff with id {}: {}", id, e.getMessage());
            return ApiResponse.notFound(e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting line tariff with id {}: {}", id, e.getMessage());
            return ApiResponse.internalServerError("Failed to delete line tariff: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/{id}/copy")
    @Operation(
            summary = "Copy line tariff to new period",
            description = "Copy existing tariff to new period. Updates source tariff PERIOD_TO to new PERIOD_FROM - 1 day and creates new tariff with copied data.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Successfully copied line tariff",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LineTariffDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or validation error",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Source line tariff not found",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Conflict - New period overlaps with existing tariff",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> copyLineTariff(
            @Parameter(description = "Source Transaction POID", required = true, example = "12345")
            @PathVariable Long id,
            @Valid @RequestBody CopyTariffRequestDTO request) {

        log.info("Copying line tariff with id: {} to new period: {} to {}", id, request.getPeriodFrom(), request.getPeriodTo());

        Long groupPoid = getGroupPoid();
        Long userPoid = getUserPoid();

        LineTariffDto copied = lineTariffsService.copyLineTariff(id, request, groupPoid, userPoid);

        log.info("Successfully copied line tariff with id: {} to new tariff with id: {}", id, copied.getTransactionPoid());
        return ApiResponse.success("Line tariff copied successfully", copied);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/{id}/copy-slabs")
    @Operation(
            summary = "Copy collectable slabs to payable",
            description = "Copy slab data from collectable to payable matched by container type. type=DMG copies import demurrage, type=DTN copies export detention.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> copySlabsToPayable(
            @PathVariable Long id,
            @RequestParam String type) {
        log.info("Copying slabs to payable for id: {}, type: {}", id, type);
        lineTariffsService.copySlabsToPayable(id, type);
        return ApiResponse.success("Slabs copied to payable successfully", null);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{id}/load-container-types")
    @Operation(
            summary = "Load available container types",
            description = "Returns container types from SHIP_LINE_MASTER_TYPE_DTL for the tariff's line, excluding already-used ones. type=IMP excludes from SHIP_LINE_TARIFF_IMP_DTL, type=EXP excludes from SHIP_LINE_TARIFF_EXP_DTL.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Container types loaded successfully",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Line tariff not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> loadContainerTypes(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long id,
            @Parameter(description = "IMP for Import Demurrage, EXP for Export Detention", required = true)
            @RequestParam String type) {
        log.info("Loading container types for id: {}, type: {}", id, type);
        lineTariffsService.loadContainerTypes(id, type);
        return ApiResponse.success("Container types loaded successfully");
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @GetMapping("/{id}/print")
    @Operation(
            summary = "Generate Notice to Trade PDF",
            description = "Generates the Notice to Trade PDF for line demurrage tariff revision with slab details grouped by container category and size.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "PDF generated successfully",
                            content = @Content(mediaType = "application/pdf")
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "Line tariff not found",
                            content = @Content(mediaType = "application/json")
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "500",
                            description = "Failed to generate PDF",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    public ResponseEntity<?> print(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long id) {
        try {
            byte[] pdf = lineTariffsService.print(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=line-tariff-notice-to-trade-" + id + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (com.asg.shipping.exceptions.ResourceNotFoundException e) {
            log.warn("Line tariff not found for print id {}: {}", id, e.getMessage());
            return ApiResponse.notFound(e.getMessage());
        } catch (ValidationException e) {
            log.warn("Line tariff print validation failed for id {}: {}", id, e.getMessage());
            return ApiResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to generate PDF for line tariff: {}", id, e);
            return ApiResponse.internalServerError("Failed to generate PDF: " + e.getMessage());
        }
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

