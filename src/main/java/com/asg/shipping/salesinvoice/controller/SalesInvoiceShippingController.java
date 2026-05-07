package com.asg.shipping.salesinvoice.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.excel.ExcelFileData;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.ExcelExportService;
import com.asg.shipping.salesinvoice.dto.*;
import com.asg.shipping.salesinvoice.service.SalesInvoiceShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;

/**
 * REST Controller for Sales Invoice Shipping operations
 */
@RestController
@RequestMapping("/v1/sales-invoice-shipping")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "sales-invoice-shipping-controller",
        description = "Manage Sales Invoice (Shipping) records with charges, containers, and demurrage details (DocId: 300-102)"
)
public class SalesInvoiceShippingController {

    private final SalesInvoiceShippingService service;
    private final ExcelExportService excelExportService;

    /**
     * Search Sales Invoice records
     * GET /v1/sales-invoice-shipping/search?docId=300-102
     */
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "List Sales Invoices with Search (DocId: 300-102)",
            description = "Fetch Sales Invoices using filters and pagination. Valid `searchField` values: GLOBALSEARCH, DOC_REF, BL_NUMBER, CUSTOMER_NAME, INV_DATE. " +
                    "Authorization Parameters (handled by interceptor): documentId (300-102), actionRequested (VIEW). " +
                    "Note: For complex filters, consider using POST /list endpoint instead."
    )
    @PostMapping("/search")
    public ResponseEntity<?> searchSalesInvoice(
            @RequestBody(required = false) FilterRequestDto request,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field and direction (e.g., 'invoiceDate,asc')")
            @RequestParam(required = false) String sort) {

        log.info("Searching sales invoices with page: {}, size: {}, sort: {}", page, size, sort);

        Pageable pageable = createPageable(page, size, sort);

        try {
            Map<String, Object> result = service.searchSalesInvoice(
                    UserContext.getDocumentId(),
                    request,
                    pageable
            );

            log.info("Successfully retrieved sales invoices");
            return success("Sales Invoice records retrieved successfully", result);

        } catch (Exception e) {
            return internalServerError("Unable to fetch Sales Invoice records: " + e.getMessage());
        }
    }

    /**
     * Get Sales Invoice by ID
     * GET /v1/sales-invoice-shipping/{id}
     */
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "Get Sales Invoice Details (DocId: 300-102)",
            description = "Fetch a single Sales Invoice with all its charges, containers, and related details.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Sales Invoice retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Sales Invoice not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<?> getSalesInvoice(
            @Parameter(description = "Sales Invoice Transaction POID", required = true, example = "12345")
            @PathVariable Long id) {
        try {
            log.info("Get request for Sales Invoice with id: {}", id);
            SalesInvoiceShippingDto dto = service.getSalesInvoice(id);
            return success("Sales Invoice retrieved successfully", dto);
        } catch (Exception e) {
            return internalServerError("Error fetching Sales Invoice: " + e.getMessage());
        }
    }

    /**
     * Create new Sales Invoice
     * POST /v1/sales-invoice-shipping
     */
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @Operation(
            summary = "Create Sales Invoice (DocId: 300-102)",
            description = "Create a new Sales Invoice record. Business Rules: Customer, BL POID, Invoice Date, and at least one charge are mandatory. Demurrage totals must match for non-EXPORT BLs.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Sales Invoice created successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request data or validation failed"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Provide full details for Sales Invoice including header, charges, and containers.",
            content = @Content(
                    schema = @Schema(implementation = SalesInvoiceShippingCreateDTO.class),
                    examples = @ExampleObject(
                            name = "Sales Invoice Create Example",
                            value = """
                                    {
                                      "customerPoid": 123,
                                      "blPoid": 456,
                                      "invDate": "2024-01-15",
                                      "blTypeInvoice": "IMPORT",
                                      "chargesDetails": [
                                        {
                                          "chargePoid": 789,
                                          "amount": 1000.00,
                                          "amountSelect": "Y",
                                          "currencyCode": "USD"
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @PostMapping("/create")
    public ResponseEntity<?> createSalesInvoice(
            @Valid @RequestBody SalesInvoiceShippingCreateDTO createDTO) {
        try {
            log.info("Create request for Sales Invoice");
            SalesInvoiceShippingDto dto = service.createSalesInvoice(createDTO);
            return success("Sales Invoice created successfully", dto);
        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Error creating Sales Invoice: " + ex.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @Operation(summary = "Export Sales Invoice to Excel")
    @GetMapping("/excel")
    public ResponseEntity<?> excelExport(
            @Parameter(description = "BL POID", required = true, example = "123")
            @RequestParam Long blPoid,
            @Parameter(description = "Currency Code", required = true, example = "USD")
            @RequestParam String currencyCode,
            @Parameter(description = "Currency Rate", required = true, example = "1.0")
            @RequestParam BigDecimal currencyRate,
            @Parameter(description = "Customer POID", required = true, example = "456")
            @RequestParam Long customerPoid,
            @Parameter(description = "Transaction POID", required = true, example = "318")
            @RequestParam Long transactionPoid) {
        try {
            BigDecimal companyPoid = service.getBillCompany(blPoid, customerPoid);
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("P_BL_POID", blPoid);
            parameters.put("P_TRANSACTION_POID", transactionPoid);
            parameters.put("P_CUR_CODE", currencyCode);
            parameters.put("P_CUR_RATE", currencyRate);
            String docId;
            String fileName;
            if (currencyCode.contains("USD")) {
                docId = "300-102";
                fileName = "Shipping_Invoice_Excel_Export_USD.xlsx";
            } else if (currencyCode.contains("BHD")) {
                docId = "100-310";
                fileName = "Shipping_Invoice_Excel_Export_BHD.xlsx";
            } else {
                docId = "300-102";
                fileName = "Shipping_Invoice_Excel_Export_USD.xlsx";
            }
            ExcelFileData data = excelExportService.generateExcel(docId, null, parameters, fileName);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + data.getFileName())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data.getContent());
        } catch (Exception e) {
            log.error("Failed to generate Excel", e);
            return ResponseEntity.status(500).body("Failed to generate Excel: " + e.getMessage());
        }
    }

    /**
     * Update existing Sales Invoice
     * PUT /v1/sales-invoice-shipping/{id}
     */
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(
            summary = "Update Sales Invoice (DocId: 300-102)",
            description = "Update an existing Sales Invoice record. Partial updates are supported - only provided fields will be updated.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Sales Invoice updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request data or validation failed"),
                    @ApiResponse(responseCode = "404", description = "Sales Invoice not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Provide fields to update. Only provided fields will be updated.",
            content = @Content(
                    schema = @Schema(implementation = SalesInvoiceShippingUpdateDTO.class),
                    examples = @ExampleObject(
                            name = "Sales Invoice Update Example",
                            value = """
                                    {
                                      "invDate": "2024-01-20",
                                      "invAmount": 1500.00,
                                      "chargesDetails": [
                                        {
                                          "detRowId": 1,
                                          "amount": 1200.00,
                                          "amountSelect": "Y"
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSalesInvoice(
            @Parameter(description = "Sales Invoice Transaction POID", required = true, example = "12345")
            @PathVariable Long id,
            @Valid @RequestBody SalesInvoiceShippingUpdateDTO updateDTO) {
        try {
            log.info("Update request for Sales Invoice with id: {}", id);
            SalesInvoiceShippingDto dto = service.updateSalesInvoice(id, updateDTO);
            return success("Sales Invoice updated successfully", dto);
        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Error updating Sales Invoice: " + ex.getMessage());
        }
    }

    /**
     * Delete Sales Invoice (soft delete)
     * DELETE /v1/sales-invoice-shipping/{id}
     */
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @Operation(
            summary = "Delete Sales Invoice (DocId: 300-102)",
            description = "Soft delete a Sales Invoice by setting the 'deleted' flag to 'Y' instead of removing it from the database.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Sales Invoice deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Sales Invoice not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSalesInvoice(
            @Parameter(description = "Sales Invoice Transaction POID", required = true, example = "12345")
            @PathVariable Long id,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {
        try {
            log.info("Delete request for Sales Invoice with id: {}", id);
            service.deleteSalesInvoice(id, deleteReasonDto);
            return success("Sales Invoice deleted successfully", null);
        } catch (Exception e) {
            return internalServerError("Error deleting Sales Invoice: " + e.getMessage());
        }
    }

    /**
     * Load container demurrage data
     * POST /v1/sales-invoice-shipping/{id}/load-container-demurrage
     */
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "Load Container Demurrage Data (DocId: 300-102)",
            description = "Load container demurrage/detention data for a BL. Calculates demurrage amounts based on container movements and tariff configurations.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Container demurrage data loaded successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request data"),
                    @ApiResponse(responseCode = "404", description = "Sales Invoice not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "BL POID and BL Type for loading container demurrage data",
            content = @Content(
                    schema = @Schema(implementation = LoadContainerDemurrageRequestDTO.class),
                    examples = @ExampleObject(
                            name = "Load Container Demurrage Example",
                            value = """
                                    {
                                      "blPoid": 456,
                                      "blTypeInvoice": "IMPORT"
                                    }
                                    """
                    )
            )
    )
    @PostMapping("/{id}/load-container-demurrage")
    public ResponseEntity<?> loadContainerDemurrage(
            @Parameter(description = "Sales Invoice Transaction POID", required = true, example = "12345")
            @PathVariable Long id,
            @Valid @RequestBody LoadContainerDemurrageRequestDTO request) {
        try {
            log.info("Load container demurrage request for invoice id: {}", id);
            LoadContainerDemurrageResponseDTO result = service.loadContainerDemurrageData(id, request);
            return success("Container demurrage data loaded successfully", result);
        } catch (Exception e) {
            return internalServerError("Error loading container demurrage data: " + e.getMessage());
        }
    }

    /**
     * Load charge data
     * POST /v1/sales-invoice-shipping/{id}/load-charge-data
     */
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "Load Charge Data (DocId: 300-102)",
            description = "Load charge data from BL Manifest charges, including late collection charges and demurrage amounts.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Charge data loaded successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request data"),
                    @ApiResponse(responseCode = "404", description = "Sales Invoice not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "BL POID and BL Type for loading charge data",
            content = @Content(
                    schema = @Schema(implementation = LoadChargeDataRequestDTO.class),
                    examples = @ExampleObject(
                            name = "Load Charge Data Example",
                            value = """
                                    {
                                      "blPoid": 456,
                                      "blTypeInvoice": "IMPORT"
                                    }
                                    """
                    )
            )
    )
    @PostMapping("/{id}/load-charge-data")
    public ResponseEntity<?> loadChargeData(
            @Parameter(description = "Sales Invoice Transaction POID", required = true, example = "12345")
            @PathVariable Long id,
            @Valid @RequestBody LoadChargeDataRequestDTO request) {
        try {
            log.info("Load charge data request for invoice id: {}", id);
            LoadChargeDataResponseDTO result = service.loadChargeData(id, request);
            return success("Charge data loaded successfully", result);
        } catch (Exception e) {
            return internalServerError("Error loading charge data: " + e.getMessage());
        }
    }

    /**
     * Validate customer
     * POST /v1/sales-invoice-shipping/validate-customer
     */
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "Validate Customer (DocId: 300-102)",
            description = "Validate customer for Sales Invoice creation. Checks credit limit, contract expiry, blocked status, and other validations.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Customer validation completed"),
                    @ApiResponse(responseCode = "400", description = "Invalid request data"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Customer POID and BL Type for validation",
            content = @Content(
                    schema = @Schema(implementation = ValidateCustomerRequestDTO.class),
                    examples = @ExampleObject(
                            name = "Validate Customer Example",
                            value = """
                                    {
                                      "customerPoid": 123,
                                      "blType": "IMPORT",
                                      "authorizedId": "AUTH001"
                                    }
                                    """
                    )
            )
    )
    @PostMapping("/validate-customer")
    public ResponseEntity<?> validateCustomer(
            @Valid @RequestBody ValidateCustomerRequestDTO request) {
        try {
            log.info("Validate customer request for customer: {}", request.getCustomerPoid());
            ValidateCustomerResponseDTO result = service.validateCustomer(request);
            return success("Customer validated successfully", result);
        } catch (Exception e) {
            return internalServerError("Error validating customer: " + e.getMessage());
        }
    }

    /**
     * Verify invoice
     * POST /v1/sales-invoice-shipping/{id}/verify
     */
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "Verify Sales Invoice (DocId: 300-102)",
            description = "Verify a Sales Invoice by setting the verified status. This is typically done by accounting department.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Invoice verified successfully"),
                    @ApiResponse(responseCode = "404", description = "Sales Invoice not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @PostMapping("/{id}/verify")
    public ResponseEntity<?> verifyInvoice(
            @Parameter(description = "Sales Invoice Transaction POID", required = true, example = "12345")
            @PathVariable Long id) {
        try {
            log.info("Verify invoice request for id: {}", id);
            service.verifyInvoice(id);
            return success("Invoice verified successfully", null);
        } catch (Exception e) {
            return internalServerError("Error verifying invoice: " + e.getMessage());
        }
    }

    /**
     * Create FF job
     * POST /v1/sales-invoice-shipping/{id}/create-ff-job
     */
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @Operation(
            summary = "Create FF Job (DocId: 300-102)",
            description = "Create a Forwarder/FF Job from Sales Invoice. This links the invoice to a forwarder job.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "FF job created successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request data"),
                    @ApiResponse(responseCode = "404", description = "Sales Invoice not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @PostMapping("/{id}/create-ff-job")
    public ResponseEntity<?> createFFJob(
            @Parameter(description = "Sales Invoice Transaction POID", required = true, example = "12345")
            @PathVariable Long id,
            @Parameter(description = "BL Manifest Transaction POID", required = true, example = "456")
            @RequestParam Long blPoid) {
        try {
            log.info("Create FF job request for invoice id: {}, BL POID: {}", id, blPoid);
            CreateFFJobResponseDTO result = service.createFFJob(id, blPoid);
            return success("FF job created successfully", result);
        } catch (Exception e) {
            return internalServerError("Error creating FF job: " + e.getMessage());
        }
    }

    /**
     * Create FF purchase journal
     * POST /v1/sales-invoice-shipping/{id}/create-ff-purchase-journal
     */
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @Operation(
            summary = "Create FF Purchase Journal (DocId: 300-102)",
            description = "Create a Purchase Journal entry for Forwarder/FF Job from Sales Invoice.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "FF purchase journal created successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request data"),
                    @ApiResponse(responseCode = "404", description = "Sales Invoice or FF Job not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @PostMapping("/{id}/create-ff-purchase-journal")
    public ResponseEntity<?> createFFPurchaseJournal(
            @Parameter(description = "Sales Invoice Transaction POID", required = true, example = "12345")
            @PathVariable Long id,
            @Parameter(description = "FF Job Transaction POID", required = true, example = "789")
            @RequestParam Long ffJobPoid) {
        try {
            log.info("Create FF purchase journal request for invoice id: {}, FF Job POID: {}", id, ffJobPoid);
            CreateFFPurchaseJournalResponseDTO result = service.createFFPurchaseJournal(id, ffJobPoid);
            return success("FF purchase journal created successfully", result);
        } catch (Exception e) {
            return internalServerError("Error creating FF purchase journal: " + e.getMessage());
        }
    }

    /**
     * Update booking party
     * POST /v1/sales-invoice-shipping/{id}/update-booking-party
     */
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(
            summary = "Update Booking Party (DocId: 300-102)",
            description = "Update the booking party for a Sales Invoice. This updates the BL Manifest booking party reference.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Booking party updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request data"),
                    @ApiResponse(responseCode = "404", description = "Sales Invoice not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Booking party POID to update",
            content = @Content(
                    schema = @Schema(implementation = UpdateBookingPartyRequestDTO.class),
                    examples = @ExampleObject(
                            name = "Update Booking Party Example",
                            value = """
                                    {
                                      "bookingPartyPoid": 999
                                    }
                                    """
                    )
            )
    )
    @PostMapping("/{id}/update-booking-party")
    public ResponseEntity<?> updateBookingParty(
            @Parameter(description = "Sales Invoice Transaction POID", required = true, example = "12345")
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingPartyRequestDTO request) {
        try {
            log.info("Update booking party request for invoice id: {}", id);
            var result = service.updateBookingParty(id, request);
            return success("Booking party updated successfully", result);
        } catch (Exception e) {
            return internalServerError("Error updating booking party: " + e.getMessage());
        }
    }

    /**
     * Get customer address
     * GET /v1/sales-invoice-shipping/customer-address/{addressMasterPoid}
     */
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "Get Customer Address (DocId: 300-102)",
            description = "Retrieve customer address details for a specific address type (INVOICE, DELIVERY, etc.).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Customer address retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Address not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/customer-address/{addressMasterPoid}")
    public ResponseEntity<?> getCustomerAddress(
            @Parameter(description = "Address Master POID", required = true, example = "111")
            @PathVariable Long addressMasterPoid,
            @Parameter(description = "Address Type (INVOICE, DELIVERY, etc.)", required = false, example = "INVOICE")
            @RequestParam(required = false, defaultValue = "INVOICE") String addressType) {
        try {
            log.info("Get customer address request for addressMasterPoid: {}, addressType: {}", addressMasterPoid, addressType);
            CustomerAddressResponseDTO result = service.getCustomerAddress(addressMasterPoid, addressType);
            return success("Customer address retrieved successfully", result);
        } catch (Exception e) {
            return internalServerError("Error fetching customer address: " + e.getMessage());
        }
    }

    /**
     * Load BL data
     * POST /v1/sales-invoice-shipping/{id}/load-bl-data
     */
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(
            summary = "Load BL Data (DocId: 300-102)",
            description = "Load BL Manifest data into Sales Invoice. This auto-populates invoice fields from the selected BL.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "BL data loaded successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request data"),
                    @ApiResponse(responseCode = "404", description = "Sales Invoice or BL not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "BL POID to load data from",
            content = @Content(
                    schema = @Schema(implementation = LoadBlDataRequestDTO.class),
                    examples = @ExampleObject(
                            name = "Load BL Data Example",
                            value = """
                                    {
                                      "blPoid": 456
                                    }
                                    """
                    )
            )
    )
    @PostMapping("/{id}/load-bl-data")
    public ResponseEntity<?> loadBlData(
            @Parameter(description = "Sales Invoice Transaction POID", required = true, example = "12345")
            @PathVariable Long id,
            @Valid @RequestBody LoadBlDataRequestDTO request) {
        try {
            log.info("Load BL data request for invoice id: {}", id);
            var result = service.loadBlData(id, request);
            return success("BL data loaded successfully", result);
        } catch (Exception e) {
            return internalServerError("Error loading BL data: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "Get Bill Company (DocId: 300-102)",
            description = "Get the bill company POID from customer master. Used for invoice printing/export to determine the correct bill company.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Bill company retrieved successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request data"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "BL POID and Customer POID to get bill company",
            content = @Content(
                    schema = @Schema(implementation = GetBillCompanyRequestDTO.class),
                    examples = @ExampleObject(
                            name = "Get Bill Company Example",
                            value = """
                                    {
                                      "blPoid": 456,
                                      "customerPoid": 123
                                    }
                                    """
                    )
            )
    )
    @PostMapping("/get-bill-company")
    public ResponseEntity<?> getBillCompany(
            @Valid @RequestBody GetBillCompanyRequestDTO request) {
        try {
            log.info("Get bill company request for BL POID: {}, Customer POID: {}", request.getBlPoid(), request.getCustomerPoid());
            GetBillCompanyResponseDTO result = service.getBillCompany(request);
            return success("Bill company retrieved successfully", result);
        } catch (Exception e) {
            return internalServerError("Error getting bill company: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "Load Print Data (DocId: 300-102)",
            description = "Load custom print data for invoice using PROC_SHIP_BL_PRINT_LOAD. " +
                    "into an array table for invoice printing.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Print data loaded successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request data"),
                    @ApiResponse(responseCode = "404", description = "Sales Invoice not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Customer POID to load print data",
            content = @Content(
                    schema = @Schema(implementation = LoadPrintDataRequestDTO.class),
                    examples = @ExampleObject(
                            name = "Load Print Data Example",
                            value = """
                                    {
                                      "customerPoid": 123
                                    }
                                    """
                    )
            )
    )
    @PostMapping("/{id}/load-print-data")
    public ResponseEntity<?> loadPrintData(
            @Parameter(description = "Sales Invoice Transaction POID", required = true, example = "12345")
            @PathVariable Long id,
            @Valid @RequestBody LoadPrintDataRequestDTO request) {
        try {
            log.info("Load print data request for invoice id: {}, customer POID: {}", id, request.getCustomerPoid());
            LoadPrintDataResponseDTO result = service.loadPrintData(id, request);
            return success(result.getMessage(), result);
        } catch (Exception e) {
            return internalServerError("Error loading print data: " + e.getMessage());
        }
    }

    private Pageable createPageable(int page, int size, String sort) {

        String sortField = "INV_DATE";
        Sort.Direction direction = Sort.Direction.DESC;

        if (sort != null && !sort.isBlank()) {
            String[] sortParams = sort.split(",");

            if (sortParams.length > 0 && !sortParams[0].isBlank()) {
                sortField = mapSortFieldToColumn(sortParams[0]);
            }

            if (sortParams.length > 1) {
                try {
                    direction = Sort.Direction.fromString(sortParams[1]);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return PageRequest.of(page, size, Sort.by(direction, sortField));
    }

    /**
     * Maps frontend sort field names to database column names
     */
    private String mapSortFieldToColumn(String sortField) {
        if (sortField == null || sortField.isBlank()) {
            return "INV_DATE";
        }

        String normalized = sortField
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toUpperCase();

        return switch (normalized) {
            case "TRANSACTION_POID" -> "TRANSACTION_POID";
            case "GROUP_POID" -> "GROUP_POID";
            case "COMPANY_POID" -> "COMPANY_POID";
            case "DOC_REF" -> "DOC_REF";
            case "TRANSACTION_DATE" -> "TRANSACTION_DATE";
            case "INV_AMOUNT" -> "INV_AMOUNT";
            case "CREATED_BY" -> "CREATED_BY";
            case "CREATED_DATE" -> "CREATED_DATE";
            case "LASTMODIFIED_BY" -> "LASTMODIFIED_BY";
            case "LASTMODIFIED_DATE" -> "LASTMODIFIED_DATE";
            default -> "INV_DATE";
        };
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @GetMapping("/print-invoice/{transactionPoid}")
    public ResponseEntity<?> printInvoice(
            @Parameter(description = "Transaction POID", example = "12345")
            @PathVariable Long transactionPoid,
            @Parameter(description = "BL POID", example = "92170", required = true)
            @RequestParam Long blPoid) {
        try {
            byte[] pdf = service.printInvoice(transactionPoid,blPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=sales-invoice-shipping-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate PDF for Journal Voucher: {}", transactionPoid, e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }

    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @GetMapping("/customer-autocharge/{transactionPoid}")
    public ResponseEntity<?> printCustomerAutoCharge(
            @Parameter(description = "Transaction POID", example = "12345")
            @PathVariable Long transactionPoid,
            @Parameter(description = "BL POID", example = "92170", required = true)
            @RequestParam Long blPoid) {
        try {
            byte[] pdf = service.printCustomerAutoCharge(transactionPoid,blPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=sales-invoice-shipping-customer-autocharge" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate PDF for Journal Voucher: {}", transactionPoid, e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }

    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @GetMapping("/print/{transactionPoid}")
    public ResponseEntity<?> print(
            @Parameter(description = "Transaction POID", example = "12345")
            @PathVariable Long transactionPoid,
            @Parameter(description = "BL POID", example = "67890")
            @RequestParam Long blPoid
    ) {
        try {
            byte[] pdf = service.print(transactionPoid, blPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=sales-invoice-shipping" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("error",e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }
}

