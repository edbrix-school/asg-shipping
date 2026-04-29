package com.asg.shipping.bookingFormSH.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.excel.ExcelFileData;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.ExcelExportService;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.bookingFormSH.dto.BookingFormAddressMasterDto;
import com.asg.shipping.bookingFormSH.dto.BookingFormCreateDTO;
import com.asg.shipping.bookingFormSH.dto.BookingFormDto;
import com.asg.shipping.bookingFormSH.dto.BookingFormUpdateDTO;
import com.asg.shipping.bookingFormSH.service.BookingFormService;
import com.asg.shipping.exceptions.ValidationException;
import com.asg.shipping.portMaster.dto.PortMasterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.StringUtil;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@RestController
@RequestMapping("/v1/booking-form-sh")
@RequiredArgsConstructor
@Slf4j
public class BookingFormController {

    private final BookingFormService bookingFormService;
    private final ExcelExportService excelExportService;
    private final LoggingService loggingService;
    private static final String FAILEDTOGENERATEPDF = "Failed to generate PDF: ";
    private static final String FAILEDTOGENERATEPDFFORBOOKINGFORM = "Failed to generate PDF for Banking Form SH: {}";

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Get all Booking Form SH", description = "Fetches all Booking Form SH records for the given group", responses = {
            @ApiResponse(responseCode = "200", description = "Booking Form records fetched successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PortMasterResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")}, security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/search")
    public ResponseEntity<?> searchBookingForm(@ParameterObject Pageable pageable,
                                               @RequestBody(required = false) FilterRequestDto filters,
                                               @RequestParam(required = false) LocalDate startDate, @RequestParam(required = false) LocalDate endDate) {

        log.info("Search request for Booking Form with docId: {}", UserContext.getDocumentId());

        if ((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
            return badRequest("Both startDate and endDate should be specified or both dates should be empty.");
        }

        Map<String, Object> result = bookingFormService.searchBookingForm(UserContext.getDocumentId(), filters,
                pageable, startDate, endDate);
        return success("Booking Form records retrieved successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Get Booking Form by POID", description = "Fetches a specific Booking Form by POID", responses = {
            @ApiResponse(responseCode = "200", description = "Booking Form retrieved successfully.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PortMasterResponse.class))),
            @ApiResponse(responseCode = "404", description = "Booking Form not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")}, security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{id}")
    public ResponseEntity<?> getBookingForm(
            @Parameter(description = "Transaction POID", required = true, example = "5001") @PathVariable Long id) {
        log.info("Get request for Booking Form with id: {}", id);
        BookingFormDto dto = bookingFormService.getBookingForm(id);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), id.toString());
        return success("Booking Form retrieved successfully.", dto);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @Operation(summary = "Create a new Booking Form", description = "Creates a new Booking Form record for the given group", responses = {
            @ApiResponse(responseCode = "200", description = "Booking Form created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")}, security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    public ResponseEntity<?> createBookingForm(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated Booking Form details", required = true) @Valid @RequestBody BookingFormCreateDTO createDTO) {
        log.info("Create request for Booking Form");
        BookingFormDto dto = bookingFormService.createBookingForm(createDTO);
        return success("Booking Form created successfully", dto);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(summary = "Update an existing Booking Form", description = "Updates the Booking Form details for the given Transaction POID", responses = {
            @ApiResponse(responseCode = "200", description = "Booking Form updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PortMasterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Booking Form not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")}, security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBookingForm(
            @Parameter(description = "Transaction POID", required = true, example = "5001") @PathVariable Long id,
            @Valid @RequestBody BookingFormUpdateDTO updateDTO) {
        log.info("Update request for Booking Form with id: {}", id);
        bookingFormService.updateBookingForm(id, updateDTO);
        BookingFormDto dto = bookingFormService.getBookingForm(id);
        return success("Booking Form updated successfully", dto);
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @Operation(summary = "Delete a Booking Form", description = "Soft deletes a Booking Form record", responses = {
            @ApiResponse(responseCode = "200", description = "Booking Form deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Booking Form not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")}, security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBookingForm(
            @Parameter(description = "Transaction POID", required = true, example = "5001") @PathVariable Long id) {
        log.info("Delete request for Booking Form with id: {}", id);
        bookingFormService.deleteBookingForm(id);
        loggingService.createLogSummaryEntry(LogDetailsEnum.DELETED, UserContext.getDocumentId(), id.toString());
        return success("Booking Form deleted successfully");
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Generate COPRAR Booking Form", description = "Generate COPRAR Booking Form", responses = {
            @ApiResponse(responseCode = "200", description = "Generate COPRAR Booking Form successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")}, security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}/generate-copran")
    public ResponseEntity<?> generateCoprarBooking(
            @Parameter(description = "Transaction POID", required = true, example = "5001") @PathVariable Long id) {
        log.info("Generate COPRAR booking file request for transaction: {}", id);
        String status = bookingFormService.generateCoprarBooking(id);
        if (status.toLowerCase().startsWith("error"))
            return error(status, 400);
        return success(status);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Get empty shipper Booking Form", description = "Get empty shipper Booking Form", responses = {
            @ApiResponse(responseCode = "200", description = "Get empty shipper Booking Form successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")}, security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/empty-shipper")
    public ResponseEntity<?> getEmptyShipper() {
        Long companyPoid = UserContext.getCompanyPoid();
        log.info("Get empty shipper request for company: {}", companyPoid);
        String shipperPoid = bookingFormService.getEmptyShipper(companyPoid);
        return success("Empty shipper retrieved successfully", shipperPoid);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(summary = "Get empty shipper Booking Form", description = "Get empty shipper Booking Form", responses = {
            @ApiResponse(responseCode = "200", description = "Get empty shipper Booking Form successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")}, security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}/process-empty-container-load")
    public ResponseEntity<?> processEmptyContainerLoad(
            @Parameter(description = "Transaction POID", required = true, example = "5001") @PathVariable Long id) {
        log.info("Process empty container load request for transaction: {}", id);
        String status = bookingFormService.processEmptyContainerLoad(id);
        if (status.toLowerCase().startsWith("error"))
            return error(status, 500);
        return success(status);
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @Operation(summary = "Generate Excel for VgmCustXLGenerateXL")
    @GetMapping("/excel/vgmCustXLGenerateXL/{transactionPoid}")
    public ResponseEntity<?> exportExcel(
            @Parameter(description = "Transaction POID", example = "476") @PathVariable Long transactionPoid) {
        try {

            ExcelFileData data = excelExportService.generateExcel("100-311", String.valueOf(transactionPoid), null,
                    "VGMCustXLFile.xlsx");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + data.getFileName())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM).body(data.getContent());
        } catch (Exception e) {
            log.error("Failed to generate Excel", e);
            return error("Failed to generate Excel: " + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @GetMapping("/mateBookingPrintForm/{transactionPoid}")
    public ResponseEntity<?> print(
            @Parameter(description = "Transaction POID", example = "21") @PathVariable Long transactionPoid) {
        try {
            byte[] pdf = bookingFormService.mateBookingPrintForm(transactionPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=container-mate-receipts-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF).body(pdf);
        } catch (Exception e) {
            log.error(FAILEDTOGENERATEPDFFORBOOKINGFORM, transactionPoid, e);
            return error(FAILEDTOGENERATEPDF + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @GetMapping("/empty-release/{transactionPoid}")
    public ResponseEntity<?> cntEmptyBookingPrintForm(
            @Parameter(description = "Transaction POID", example = "21") @PathVariable Long transactionPoid) {
        try {
            byte[] pdf = bookingFormService.cntEmptyBookingPrintForm(transactionPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=container-empty-release-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF).body(pdf);
        } catch (Exception e) {
            log.error(FAILEDTOGENERATEPDFFORBOOKINGFORM, transactionPoid, e);
            return error(FAILEDTOGENERATEPDF + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @GetMapping("/all-container/{transactionPoid}")
    public ResponseEntity<?> cntReturnBookingPrintFormAll(
            @Parameter(description = "Transaction POID", example = "21") @PathVariable Long transactionPoid,
            @Parameter(description = "Print Stamp", example = "Y") @RequestParam String printStamp) {
        try {
            byte[] pdf = bookingFormService.cntReturnBookingPrintFormAll(transactionPoid, printStamp);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=all-container-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF).body(pdf);
        } catch (Exception e) {
            log.error(FAILEDTOGENERATEPDFFORBOOKINGFORM, transactionPoid, e);
            return error(FAILEDTOGENERATEPDF + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @GetMapping("/container/{transactionPoid}")
    public ResponseEntity<?> cntReturnBookingPrintForm(
            @Parameter(description = "Transaction POID", example = "21") @PathVariable Long transactionPoid,
            @Parameter(description = "Print Stamp", example = "Y") @RequestParam String printStamp) {
        try {
            byte[] pdf = bookingFormService.cntReturnBookingPrintForm(transactionPoid, printStamp, null);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=container-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF).body(pdf);
        } catch (Exception e) {
            log.error(FAILEDTOGENERATEPDFFORBOOKINGFORM, transactionPoid, e);
            return error(FAILEDTOGENERATEPDF + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @GetMapping("/container-individual/{transactionPoid}")
    public ResponseEntity<?> cntReturnBookingPrintFormIndividual(
            @Parameter(description = "Transaction POID", example = "21") @PathVariable Long transactionPoid,
            @Parameter(description = "Print Stamp", example = "Y") @RequestParam String printStamp,
            @Parameter(description = "Container Number") @RequestParam(required = true) String containerNo) {
        try {
            if (StringUtil.isBlank(containerNo)) throw new ValidationException("Container number is required");
            byte[] pdf = bookingFormService.cntReturnBookingPrintForm(transactionPoid, printStamp, containerNo);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=container-individual" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF).body(pdf);
        } catch (Exception e) {
            log.error(FAILEDTOGENERATEPDFFORBOOKINGFORM, transactionPoid, e);
            return error(FAILEDTOGENERATEPDF + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Search Container Inventory Empty In",
            description = "Fetches paginated records from VW_CONTAINER_INVENTORY_EMPTYIN with filter and search support. " +
                    "Use 'filters' array for field-level filtering (BL_NUMBER, CONTAINER_NO, LINE, EQUIPMENT_ISO_TYPE) " +
                    "or 'searchField'/'searchValue' for single field / GLOBALSEARCH.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Records retrieved successfully",
                            content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            },
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/container-inventory/search")
    public ResponseEntity<?> searchContainerInventory(
            @ParameterObject Pageable pageable,
            @RequestParam(required = false) String containerNo,
            @RequestParam(required = false) String isoType,
            @RequestParam(required = true) Long linePoid) {

        Map<String, Object> result = bookingFormService.searchContainerInventory(
                UserContext.getDocumentId(), containerNo, isoType, linePoid, pageable);

        return success("Container inventory records retrieved successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "Get Customer Address (DocId: 100-140)",
            description = "Retrieve customer address details for a specific address type (MAIN, DELIVERY, etc.).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Customer address retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Address not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/customer-address/{addressMasterPoid}")
    public ResponseEntity<?> getCustomerAddress(
            @Parameter(description = "Address Master POID", required = true, example = "111")
            @PathVariable Long addressMasterPoid,
            @Parameter(description = "Address Type (MAIN, DELIVERY, etc.)", required = false, example = "MAIN")
            @RequestParam(required = false, defaultValue = "MAIN") String addressType) {
        try {
            log.info("Get customer address request for addressMasterPoid: {}, addressType: {}", addressMasterPoid, addressType);
            BookingFormAddressMasterDto result = bookingFormService.getCustomerAddress(addressMasterPoid, addressType);
            return success("Customer address retrieved successfully", result);
        } catch (Exception e) {
            return internalServerError("Error fetching customer address: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "Get Customer Address (DocId: 100-140)",
            description = "Retrieve Datas From the Container Based on the Split Values",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Transfer Datas retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Transfer Data Not FOund not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/transfer/{transactionPoid}")
    public ResponseEntity<?> transferBooking(
            @PathVariable Long transactionPoid) {
        log.info("Transfer poid: " + transactionPoid);
        Map<String, Object> result =
                bookingFormService.transferBookingWithContainers(transactionPoid);

        return success("Booking Form retrieved successfully.", result);
    }


    @Operation(
            summary = "Import file",
            description = "Import Excel file containing TDR details using PROC_PDA_IMPORT_TDR_DETAIL2 stored procedure.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Successfully imported TDR file",
                            content = @Content(mediaType = "application/json")
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Invalid file or import error",
                            content = @Content(mediaType = "application/json")
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized - Authentication required",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping(value = "/{transactionPoid}/container-details/import-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importTdrFile(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid,
            @Parameter(description = "Excel file containing Container details", required = true)
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file
    ) {
        String result = bookingFormService.importFileWithTransaction(file, transactionPoid, UserContext.getGroupPoid(), UserContext.getCompanyPoid(), UserContext.getUserPoid());
        return success(result, null);
    }

}
