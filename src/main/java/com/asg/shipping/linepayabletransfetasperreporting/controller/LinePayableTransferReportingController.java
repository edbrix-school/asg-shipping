package com.asg.shipping.linepayabletransfetasperreporting.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.common.ApiResponse;
import com.asg.shipping.linepayabletransfetasperreporting.dto.*;
import com.asg.shipping.linepayabletransfetasperreporting.service.LinePayableTransferReportingService;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Line Payable Transfer As Per Reporting (THC/FRT) operations
 */
@RestController
@RequestMapping("/v1/line-payable-transfer-reporting")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Line Payable Transfer Reporting", description = "APIs for managing Line Payable Transfer As Per Reporting (THC/FRT)")
public class LinePayableTransferReportingController {

    private final LinePayableTransferReportingService service;
    private final LoggingService loggingService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/search")
    @Operation(
            summary = "Search line payable transfer records",
            description = "Retrieve paginated list of line payable transfer records with optional filtering and sorting",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved records",
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
    public ResponseEntity<?> searchLinePayableTransfer(
            @RequestBody(required = false) FilterRequestDto filterRequest,
            @ParameterObject Pageable pageable) {
        log.info("Searching line payable transfer records with page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        String docId = "100-432";
        Map<String, Object> result = service.searchLinePayableTransfer(docId, filterRequest, pageable);
        log.info("Successfully retrieved line payable transfer records");
        return ApiResponse.success("Line payable transfer records retrieved successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/load-data")
    @Operation(
            summary = "Load data by date range (before create)",
            description = "Load line payable transfer details by date range without transaction ID",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully loaded data",
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
    public ResponseEntity<?> loadDataBeforeCreate(
            @Parameter(description = "Date range request", required = true)
            @Valid @RequestBody LoadDataByDateRangeRequest request) {
        log.info("Loading data by date range before create");
        List<LinePayableTransferReportingDtlDto> details = service.loadDataBeforeCreate(request);
        log.info("Successfully loaded {} detail records", details.size());
        return ApiResponse.success("Data loaded successfully", details);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/process-weekly")
    @Operation(
            summary = "Process weekly BL report (before create)",
            description = "Process weekly BL report data without transaction ID",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully processed weekly report",
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
    public ResponseEntity<?> processWeeklyBeforeCreate(
            @Parameter(description = "Date range request", required = true)
            @Valid @RequestBody LoadDataByDateRangeRequest request) {
        log.info("Processing weekly BL report before create");
        List<LinePayableTransferReportingDtlDto> details = service.processWeeklyBeforeCreate(request);
        log.info("Successfully processed {} weekly report records", details.size());
        return ApiResponse.success("Weekly BL report processed successfully", details);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    @Operation(
            summary = "Get line payable transfer details",
            description = "Retrieve complete line payable transfer information by ID",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved record",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LinePayableTransferReportingDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Record not found",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> getLinePayableTransfer(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long transactionPoid) {
        log.info("Getting line payable transfer with id: {}", transactionPoid);
        LinePayableTransferReportingDto dto = service.getLinePayableTransferById(transactionPoid);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
        log.info("Successfully retrieved line payable transfer with id: {}", transactionPoid);
        return ApiResponse.success("Line payable transfer retrieved successfully", dto);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    @Operation(
            summary = "Create line payable transfer",
            description = "Create a new line payable transfer record",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully created record",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LinePayableTransferReportingDto.class)
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
    public ResponseEntity<?> createLinePayableTransfer(
            @Parameter(description = "Line payable transfer creation data", required = true)
            @Valid @RequestBody LinePayableTransferReportingCreateDTO createDTO) {
        log.info("Creating line payable transfer, groupId: {}, userPoid: {}",
                UserContext.getGroupPoid(), UserContext.getUserPoid());
        LinePayableTransferReportingDto dto = service.createLinePayableTransfer(createDTO);
        log.info("Successfully created line payable transfer with id: {}", dto.getTransactionPoid());
        return ApiResponse.success("Line payable transfer created successfully", dto);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}")
    @Operation(
            summary = "Update line payable transfer",
            description = "Update an existing line payable transfer record",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully updated record",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LinePayableTransferReportingDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Record not found",
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
    public ResponseEntity<?> updateLinePayableTransfer(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long transactionPoid,
            @Parameter(description = "Line payable transfer update data", required = true)
            @Valid @RequestBody LinePayableTransferReportingUpdateDTO updateDTO) {
        log.info("Updating line payable transfer with id: {}, groupId: {}, userPoid: {}",
                transactionPoid, UserContext.getGroupPoid(), UserContext.getUserPoid());
        LinePayableTransferReportingDto dto = service.updateLinePayableTransfer(transactionPoid, updateDTO);
        log.info("Successfully updated line payable transfer with id: {}", transactionPoid);
        return ApiResponse.success("Line payable transfer updated successfully", dto);
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}")
    @Operation(
            summary = "Delete line payable transfer",
            description = "Soft delete a line payable transfer record",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully deleted record",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Record not found",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> deleteLinePayableTransfer(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long transactionPoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {
        log.info("Deleting line payable transfer with id: {}", transactionPoid);
        service.deleteLinePayableTransfer(transactionPoid, deleteReasonDto);
        log.info("Successfully deleted line payable transfer with id: {}", transactionPoid);
        return ApiResponse.success("Line payable transfer deleted successfully");
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/{transactionPoid}/load-data")
    @Operation(
            summary = "Load data by date range",
            description = "Load line payable transfer details by date range",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully loaded data",
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
    public ResponseEntity<?> loadDataByDateRange(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long transactionPoid,
            @Parameter(description = "Date range request", required = true)
            @Valid @RequestBody LoadDataByDateRangeRequest request) {
        log.info("Loading data by date range for transaction: {}", transactionPoid);
        List<LinePayableTransferReportingDtlDto> details = service.loadDataByDateRange(transactionPoid, request);
        log.info("Successfully loaded {} detail records", details.size());
        return ApiResponse.success("Data loaded successfully", details);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/{transactionPoid}/process-weekly")
    @Operation(
            summary = "Process weekly BL report",
            description = "Process weekly BL report data using PROC_weekly_bl_report",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully processed weekly report",
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
    public ResponseEntity<?> processWeeklyBlReport(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long transactionPoid,
            @Parameter(description = "Date range request", required = true)
            @Valid @RequestBody LoadDataByDateRangeRequest request) {
        log.info("Processing weekly BL report for transaction: {}", transactionPoid);
        List<LinePayableTransferReportingDtlDto> details = service.processWeeklyBlReport(transactionPoid, request);
        log.info("Successfully processed {} weekly report records", details.size());
        return ApiResponse.success("Weekly BL report processed successfully", details);
    }

}