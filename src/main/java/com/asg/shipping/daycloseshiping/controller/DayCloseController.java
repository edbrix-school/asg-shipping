package com.asg.shipping.daycloseshiping.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.daycloseshiping.dto.DayCloseDto;
import com.asg.shipping.daycloseshiping.dto.DayCloseHdrDto;
import com.asg.shipping.daycloseshiping.dto.DayCloseSummaryProjection;
import com.asg.shipping.daycloseshiping.service.DayCloseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@RestController
@RequestMapping("/v1/day-close-shipping")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Day close (Shipping)", description = "APIs for Shipping Day Close (Shipping)")
public class DayCloseController {

    private final DayCloseService dayCloseService;
    private final LoggingService loggingService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    @Operation(summary = "Get Day close details", description = "Fetch day close details for the poid", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> getDayClose(
            @Parameter(description = "Day Close Transaction POID", example = "100023") @PathVariable(name = "transactionPoid") Long transactionPoid) {

        DayCloseDto response = dayCloseService.getDayClose(transactionPoid, UserContext.getGroupPoid(),
                UserContext.getCompanyPoid());
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());

        return success("Day close fetched successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @GetMapping("/new")
    @Operation(summary = "Get new Day Close data", description = "Fetch pending day close date and consolidated cash/cheque summary", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> getNewDayClose(
            @Parameter(description = "Transaction Date", example = "2025-07-06") @RequestParam(name = "transactionDate") String transactionDate) {

        DayCloseSummaryProjection response = dayCloseService.getNewDayCloseData(UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(), transactionDate);

        return success("New day Close data fetched successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/denominations")
    @Operation(summary = "Get cash denominations", description = "Fetch denomination details for selected currency", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> getDenominations(
            @Parameter(description = "Currency Code", example = "BHD", name = "currencyCode") @RequestParam(name = "currencyCode") String currencyCode) {

        List<Map<String, Object>> denominations = dayCloseService.getDenominations(currencyCode);

        return success("Denominations fetched successfully", denominations);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    @Operation(summary = "Create Day Close", description = "Create Day Close header and denomination details", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> createdayClose(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Day close details to be created", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = DayCloseDto.class))) @Valid @RequestBody DayCloseDto request) {
        Long groupPoid = request.getHeader().getGroupPoid();
        Long companyPoid = request.getHeader().getCompanyPoid();
        DayCloseDto response = dayCloseService.createDayClose(request, groupPoid, companyPoid, UserContext.getUserPoid());
        return success("Day Close created successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/update/{transactionPoid}")
    @Operation(summary = "Save Day Close", description = "Save Day Close header and denomination details", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> updateDayClose(
            @Parameter(description = "Day Close Transaction POID", example = "100023") @PathVariable(name = "transactionPoid") Long transactionPoid,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Day close details to be updated", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = DayCloseDto.class))) @Valid @RequestBody DayCloseDto request) {

        DayCloseDto response = dayCloseService.updateDayClose(request, transactionPoid, UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(), UserContext.getUserPoid());

        return success("Day Close updated successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete DayClose Shipping")
    public ResponseEntity<?> deleteCollectionHandover(
            @Parameter(description = "Dayclose ID to delete", required = true)
            @PathVariable(name = "id") Long id,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {
        log.info("Deleting DayClose Shipping with id: {}", id);
        dayCloseService.deleteDayClose(id,deleteReasonDto);
        return success("DayClose Shipping deleted successfully");
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Get all Day close", description = "Fetches all day close records for the given group", responses = {
            @ApiResponse(responseCode = "200", description = "Day close fetched successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")}, security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/search")
    public ResponseEntity<?> searchDayClose(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters,
            @RequestParam(name = "startDate", required = false) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) LocalDate endDate
    ) {
        if ((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
            return badRequest("Both startDate and endDate should be specified or both dates should be empty.");
        }
        Map<String, Object> response = dayCloseService.searchDayClose(UserContext.getDocumentId(), filters, pageable, startDate, endDate);
        return success("Day Close list fetched successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @GetMapping("/print/{transactionPoid}")
    @Operation(summary = "Print Day Close main report", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> printDayClose(
            @Parameter(description = "Transaction POID", example = "69789")
            @PathVariable Long transactionPoid) {
        try {
            byte[] pdf = dayCloseService.printDayClose(transactionPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=day-close-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate day close PDF: {}", transactionPoid, e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @GetMapping("/print/{transactionPoid}/details")
    @Operation(summary = "Print Day Close details report", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> printDetails(
            @Parameter(description = "Transaction POID", example = "69789")
            @PathVariable Long transactionPoid) {
        try {
            byte[] pdf = dayCloseService.printDetails(transactionPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=day-close-details-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate details PDF: {}", transactionPoid, e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @GetMapping("/print/{transactionPoid}/split-receipt")
    @Operation(summary = "Print Day Close split receipt report", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> printSplitReceipt(
            @Parameter(description = "Transaction POID", example = "69789")
            @PathVariable Long transactionPoid) {
        try {
            byte[] pdf = dayCloseService.printSplitReceipt(transactionPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=day-close-split-receipt-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate split receipt PDF: {}", transactionPoid, e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @GetMapping("/print/{transactionPoid}/summary")
    @Operation(summary = "Print Day Close summary report", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> printSummary(
            @Parameter(description = "Transaction POID", example = "69789")
            @PathVariable Long transactionPoid) {
        try {
            byte[] pdf = dayCloseService.printSummary(transactionPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=day-close-summary-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate summary PDF: {}", transactionPoid, e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }
}
