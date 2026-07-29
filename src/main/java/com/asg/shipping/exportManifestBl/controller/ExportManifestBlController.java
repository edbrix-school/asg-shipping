package com.asg.shipping.exportManifestBl.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.response.ApiResponse;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDownloadHeaderService;
import com.asg.shipping.exportManifestBl.dto.*;
import com.asg.shipping.exportManifestBl.entity.ExportManifestBlHdr;
import com.asg.shipping.exportManifestBl.service.ExportManifestBlService;
import com.asg.shipping.exportManifestUpdate.dto.GenerateBlPrintRequest;
import com.asg.shipping.exportManifestUpdate.dto.GenerateManifestRequest;
import com.asg.shipping.importmanifestbl.dto.ChargeDefaultsRequestDto;
import com.asg.shipping.importmanifestbl.dto.ChargeDefaultsResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;
import static com.asg.common.lib.security.util.UserContext.getCompanyPoid;
import static com.asg.common.lib.security.util.UserContext.getGroupPoid;

@RestController
@RequestMapping("/v1/export-manifest-bl")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Export Manifest BL Management", description = "APIs for managing Export Manifest BL records")
public class ExportManifestBlController {

    private final ExportManifestBlService service;
    private final DocumentDownloadHeaderService downloadHeaderService;

    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = """
                    - ### Filters:
                      Use either:
                      1. A single `GLOBALSEARCH` filter, OR
                      2. Any combination of specific fields (BL_NUMBER, TRANSACTION_POID, CREATED_BY, etc.).
                      3. operator field will either have "AND" or "OR", if not given will be considered as "OR",
                         not required for GLOBALSEARCH, for non GLOBALSEARCH need to give only 1 time
                      4. isDeleted when 'Y' or null, will search and return non deleted records, 'Y' will check and return deleted records
                      5. sort will be default on primary key ascending if specified in db field otherwise you can override it giving the field name and the direction.
                    
                    - #### Global Search:
                      Apply one search term across multiple fields.
                      No operator need to send in this case.
                      • { "searchField": "GLOBALSEARCH", "searchValue": "United" }
                    
                    - #### Single Field, Single Value:
                      Search one field with one value.
                      • { "searchField": "BL_NUMBER", "searchValue": "HJSCSHZJ40741500" },
                    
                    - #### Single Field, Multiple Values:
                      Provide multiple values for the same field, separated by `|`.
                      • { "searchField": "TRANSACTION_POID", "searchValue": 242834 }
                    
                    - #### Multiple Different Fields, Single or Multiple Value:
                      Provide one or multiple search values across multiple fields.
                      • { "searchField": "BL_NUMBER", "searchValue": "HJSCSHZJ40741500" },
                      • { "searchField": "CREATED_BY", "searchValue": "ADMIN|USER" }
                      • "operator" :  "AND"/"OR"
                    
                    - ### Sorting:
                      Defaults to whatever specified in list_of_records_sql field mostly primary key ascending.
                      To override, pass `sort=<field>,ASC|DESC` in query params.
                      Examples:
                      • sort=BL_NUMBER,ASC
                      • sort=TRANSACTION_POID,DESC
                    """,
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "Export Manifest Filters",
                                    value = """
                                            {
                                            "operator": "AND",
                                            "isDeleted": "N",
                                            "filters": [
                                               { "searchField": "GLOBALSEARCH", "searchValue": "United" },
                                               { "searchField": "BL_NUMBER", "searchValue": "HJSCSHZJ40741500" },
                                               { "searchField": "TRANSACTION_POID", "searchValue": 242834},
                                               { "searchField": "CREATED_BY", "searchValue": "ADMIN"}
                                            ]
                                            }
                                            """
                            )
                    }
            )
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/List")
    @Operation(
            summary = "Search Export Manifest BL records",
            description = "Search Export Manifest BL records with pagination, filtering, and sorting. Only returns records where BL_TYPE = 'EXPORT'.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved Export Manifest BL list",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Insufficient permissions",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> searchExportManifestBl(
            @Parameter(description = "Document ID for search configuration", required = true, example = "TBD")
            @RequestParam(required = false) String docId,
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters,
            @Parameter(description = "Start date (inclusive) for transaction date filter, format: yyyy-MM-dd")
            @RequestParam(required = false) LocalDate fromDate,
            @Parameter(description = "End date (inclusive) for transaction date filter, format: yyyy-MM-dd")
            @RequestParam(required = false) LocalDate toDate) {
        try {
            String documentId = docId != null ? docId : UserContext.getDocumentId();
            Map<String, Object> result = service.searchExportManifestBl(documentId, filters, fromDate, toDate, pageable);
            return success("Export Manifest BL list fetched successfully", result);
        } catch (Exception e) {
            log.error("Error fetching Export Manifest BL List", e);
            return internalServerError("Error fetching Export Manifest BL List: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/voyage/{issueVesselVoyagePoid}/booking-selection")
    @Operation(
            summary = "Select Booking popup grid (legacy VwPendingMateToBlView1)",
            description = """
                    **Legacy mapping (Eblmanifestpagebn.loadDataBooking → VwPendingMateToBlView1):**
                    - Path `{issueVesselVoyagePoid}` = **Issue Vessel Voyage** (`pVoyageVesselPoid1`) — line default and `FUNC_LOAD_BOOKING_TO_BL`; not used to filter the export popup grid.
                    - Query `bookingMateVoyageNo` = **Booking Mate Voyage** (`inputVoyageLoad`) — filters `VOYAGE_NO`.
                    - Query `linePoid` = optional; when omitted, line from issue voyage (`SHIP_VOYAGE_HDR.LINE_POID`); `0` = no line filter.
                    - Data source: `VW_PENDING_MATE_TO_BL` only.
                    - Search: `containerNo`, `bookingNo` or alias `bookingIssueNo`.
                    
                    **Load Selected:** `POST /voyage/{issueVesselVoyagePoid}/load-booking` with `selections` or `selectedBookingIds`; then `GET /{transactionPoid}` for header/containers/general.
                    """
    )
    public ResponseEntity<?> listBookingSelection(
            @Parameter(description = "Issue Vessel Voyage POID (SHIP_VOYAGE_HDR.TRANSACTION_POID)", required = true)
            @PathVariable Long issueVesselVoyagePoid,
            @Parameter(description = "Booking Mate Voyage — voyage number filter for popup (inputVoyageLoad)")
            @RequestParam(required = false) String bookingMateVoyageNo,
            @Parameter(description = "Line POID; defaults to line on issue voyage when omitted")
            @RequestParam(required = false) Long linePoid,
            @Parameter(description = "Container number search (partial match)")
            @RequestParam(required = false) String containerNo,
            @Parameter(description = "Booking issue number search (partial match)")
            @RequestParam(required = false) String bookingNo,
            @Parameter(description = "Alias for bookingNo (legacy query param name)")
            @RequestParam(required = false) String bookingIssueNo) {
        try {
            String effectiveBookingNo = StringUtils.hasText(bookingNo) ? bookingNo : bookingIssueNo;
            List<BookingSelectionRowDto> rows = service.listBookingSelection(
                    issueVesselVoyagePoid, bookingMateVoyageNo, linePoid, containerNo, effectiveBookingNo);
            return success("Booking selection list fetched successfully", rows);
        } catch (ValidationException e) {
            log.warn("Booking selection validation failed for voyage {}: {}", issueVesselVoyagePoid, e.getMessage());
            return error(e.getMessage(), 400);
        } catch (ResourceNotFoundException e) {
            return error(e.getMessage(), 404);
        } catch (Exception e) {
            log.error("Failed to list booking selection for voyage {}", issueVesselVoyagePoid, e);
            return error("Error fetching booking selection: " + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    @Operation(
            summary = "Get Export Manifest BL record details",
            description = "Retrieve complete Export Manifest BL record information by ID including all detail records",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved Export Manifest BL record",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ExportManifestBlRequestDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Export Manifest BL record not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> getExportManifestBl(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long transactionPoid) {

        log.info("Getting Export Manifest BL with transactionPoid: {}", transactionPoid);
        ExportManifestBlRequestDto manifestBl = service.getExportManifestBl(transactionPoid);
        log.info("Successfully retrieved Export Manifest BL with transactionPoid: {}", transactionPoid);
        return ApiResponse.success("Export Manifest BL retrieved successfully", manifestBl);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    @Operation(
            summary = "Create Export Manifest BL record",
            description = "Create a new Export Manifest BL record with validation. BL_TYPE is automatically set to 'EXPORT'.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully created Export Manifest BL record",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ExportManifestBlRequestDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or validation error",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "Business rule violation (e.g., BL number already exists)",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> createExportManifestBl(
            @Valid @RequestBody ExportManifestBlCreateDto dto) {

        log.info("Creating new Export Manifest BL");
        ExportManifestBlRequestDto created = service.createExportManifestBl(dto);
        log.info("Successfully created Export Manifest BL with transactionPoid: {}", created.getTransactionPoid());
        return ApiResponse.success("Export Manifest BL created successfully", created);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}")
    @Operation(
            summary = "Update Export Manifest BL record",
            description = "Update an existing Export Manifest BL record with validation",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully updated Export Manifest BL record",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ExportManifestBlRequestDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or validation error",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Export Manifest BL record not found",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "Business rule violation",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> updateExportManifestBl(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long transactionPoid,
            @Valid @RequestBody ExportManifestBlUpdateDto dto) {

        log.info("Updating Export Manifest BL with transactionPoid: {}", transactionPoid);

        Long companyPoid = getCompanyPoid();
        Long groupPoid = getGroupPoid();

        ExportManifestBlRequestDto updated = service.updateExportManifestBl(transactionPoid, dto, companyPoid, groupPoid);

        log.info("Successfully updated Export Manifest BL with transactionPoid: {}", transactionPoid);
        return ApiResponse.success("Export Manifest BL updated successfully", updated);
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}")
    @Operation(
            summary = "Delete Export Manifest BL record",
            description = "Soft delete an Export Manifest BL record by setting DELETED='Y'",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully deleted Export Manifest BL record"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Export Manifest BL record not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> deleteExportManifestBl(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long transactionPoid) {

        log.info("Deleting Export Manifest BL with transactionPoid: {}", transactionPoid);
        service.deleteExportManifestBl(transactionPoid);
        log.info("Successfully deleted Export Manifest BL with transactionPoid: {}", transactionPoid);
        return ApiResponse.success("Export Manifest BL deleted successfully");
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}/ff-jobs")
    @Operation(
            summary = "List FF jobs for Export Manifest BL",
            description = "Retrieve FF job rows from VW_SHIP_BL_TO_FF for the FF Jobs tab (read-only grid).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getShipBlToFfByManifestPoid(
            @Parameter(description = "Export Manifest BL transaction POID", required = true, example = "249416")
            @PathVariable Long transactionPoid) {

        log.info("Getting FF job list for manifest transactionPoid: {}", transactionPoid);
        List<ShipBlToFfDto> result = service.getShipBlToFfByManifestPoid(transactionPoid);
        log.info("Successfully retrieved {} FF job row(s) for manifest transactionPoid: {}", result.size(), transactionPoid);
        return ApiResponse.success("FF job details retrieved successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}/ff-jobs/{rnumid}")
    @Operation(
            summary = "Delete FF purchase journal for a specific FF Jobs row",
            description = "Reverses FF PJ for the selected row via PROC_GL_REVERSE_SHTOFF_POSTING. FF invoice is retained (VAT rule).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> deleteFfPurchaseJournal(
            @Parameter(description = "Export Manifest BL transaction POID", required = true, example = "268427")
            @PathVariable Long transactionPoid,
            @Parameter(description = "FF Jobs row id (RNUMID)", required = true, example = "43")
            @PathVariable Long rnumid) {

        log.info("Deleting FF purchase journal for manifest transactionPoid: {}, rnumid: {}", transactionPoid, rnumid);
        service.deleteFfPurchaseJournal(transactionPoid, rnumid);
        log.info("Successfully deleted FF purchase journal for manifest transactionPoid: {}, rnumid: {}", transactionPoid, rnumid);
        return ApiResponse.success("FF purchase journal deleted successfully");
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/ff-job/{blNumber}")
    @Operation(
            summary = "Get FF job details by BL number",
            description = "Retrieve FF job details from VW_SHIP_BL_TO_FF view based on BL number",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved FF job details",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ShipBlToFfDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "FF job details not found for the given BL number",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> getShipBlToFfByBlNumber(
            @Parameter(description = "BL Number", required = true, example = "HJSCSHZJ40741500")
            @PathVariable String blNumber) {

        log.info("Getting FF job details for BL number: {}", blNumber);
        ShipBlToFfDto result = service.getShipBlToFfByBlNumber(blNumber);
        log.info("Successfully retrieved FF job details for BL number: {}", blNumber);
        return ApiResponse.success("FF job details retrieved successfully", result);
    }
    
    @AllowedAction(UserRolesRightsEnum.PRINT)
	@Operation(summary = "Generate BL Print", description = "Generates BL print (original or draft)")
	@Parameters({
			@Parameter(name = "X-Document-Id", in = ParameterIn.HEADER, description = "Document identifier required for auditing purposes.", example = "100-352", required = true, schema = @Schema(type = "string", example = "100-352")),
			@Parameter(name = "X-Action-Requested", in = ParameterIn.HEADER, description = "Action requested must match this endpoint's @AllowedAction (VIEW).", example = "VIEW", required = true, schema = @Schema(type = "string", example = "VIEW")) })
	@PostMapping("/{transactionPoid}/generate-bl-print")
	public ResponseEntity<?> generateBlPrint(
			@Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Generate BL print request", required = true) @Valid @RequestBody GenerateBlPrintRequest request) {
		try {
			String docId = UserContext.getDocumentId();
			byte[] pdf = service.generateBlPrint(transactionPoid, request, docId);
			return ResponseEntity.ok()
					.headers(downloadHeaderService.buildAttachmentHeaders(
							ExportManifestBlHdr.class, transactionPoid, "bl-print", "pdf"))
					.contentType(MediaType.APPLICATION_PDF).body(pdf);
		} catch (ValidationException e) {
			log.warn("Validation failed for BL Print {}: {}", transactionPoid, e.getMessage());
			return error(e.getMessage(), 400);
		} catch (Exception e) {
			log.error("Failed to generate PDF for BL Print: {}", transactionPoid, e);
			return error("Failed to generate PDF: " + e.getMessage(), 500);
		}
	}

	@AllowedAction(UserRolesRightsEnum.PRINT)
	@Operation(summary = "Generate Manifest", description = "Generates cargo manifest or freight manifest")
	@Parameters({
			@Parameter(name = "X-Document-Id", in = ParameterIn.HEADER, description = "Document identifier required for auditing purposes.", example = "100-352", required = true, schema = @Schema(type = "string", example = "100-352")),
			@Parameter(name = "X-Action-Requested", in = ParameterIn.HEADER, description = "Action requested must match this endpoint's @AllowedAction (VIEW).", example = "VIEW", required = true, schema = @Schema(type = "string", example = "VIEW")) })
	@PostMapping("/{transactionPoid}/generate-manifest")
	public ResponseEntity<?> generateManifest(
			@Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Generate manifest request", required = true) @Valid @RequestBody GenerateManifestRequest request) {
		try {
			String docId = UserContext.getDocumentId();
			byte[] pdf = service.generateManifest(transactionPoid, request, docId);
			String filePrefix = request.isCargoManifest() ? "cargo-manifest" : "freight-manifest";
			return ResponseEntity.ok()
					.headers(downloadHeaderService.buildAttachmentHeaders(
							ExportManifestBlHdr.class, transactionPoid, filePrefix, "pdf"))
					.contentType(MediaType.APPLICATION_PDF).body(pdf);
		} catch (Exception e) {
			log.error("Failed to generate PDF for Day Close Shipping: {}", transactionPoid, e);
			return error("Failed to generate PDF: " + e.getMessage(), 500);
		}
	}

	@AllowedAction(UserRolesRightsEnum.PRINT)
	@Operation(summary = "Generate Detention/Storage Report", description = "Generates detention/storage report")
	@Parameters({
			@Parameter(name = "X-Document-Id", in = ParameterIn.HEADER, description = "Document identifier required for auditing purposes.", example = "100-352", required = true, schema = @Schema(type = "string", example = "100-352")),
			@Parameter(name = "X-Action-Requested", in = ParameterIn.HEADER, description = "Action requested must match this endpoint's @AllowedAction (VIEW).", example = "VIEW", required = true, schema = @Schema(type = "string", example = "VIEW")) })
	@PostMapping("/{transactionPoid}/generate-detention-storage")
	public ResponseEntity<?> generateDetentionStorage(
			@Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
		try {
			String docId = UserContext.getDocumentId();
			byte[] pdf = service.generateDetentionStorage(transactionPoid, docId);
			return ResponseEntity.ok()
					.headers(downloadHeaderService.buildAttachmentHeaders(
							ExportManifestBlHdr.class, transactionPoid, "detention-storage", "pdf"))
					.contentType(MediaType.APPLICATION_PDF).body(pdf);
		} catch (Exception e) {
			log.error("Failed to generate PDF for detention or port storage: {}", transactionPoid, e);
			return error("Failed to generate PDF: " + e.getMessage(), 500);
		}
	}
	
	@AllowedAction(UserRolesRightsEnum.PRINT)
	@Operation(summary = "Generate PDF for Draft Invoice", description = "Generate PDF report for a specific Draft Invoice")
	@GetMapping("/exportDraftInvoice/{transactionPoid}")
	public ResponseEntity<?> exportDraftPrint(@PathVariable Long transactionPoid) {
		try {
			byte[] pdf = service.exportDraftPrint(transactionPoid);
			return ResponseEntity.ok()
					.headers(downloadHeaderService.buildAttachmentHeaders(
							ExportManifestBlHdr.class, transactionPoid, "draft-invoice-em", "pdf"))
					.contentType(MediaType.APPLICATION_PDF).body(pdf);
		} catch (Exception e) {
			log.error("Failed to generate PDF for Draft Invoice: {}", transactionPoid, e);
			return ApiResponse.error("Failed to generate PDF: " + e.getMessage(), 500);
		}
	}

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "Get Charge Tax Defaults",
            description = "Fetch tax POID and tax percentage for a selected charge. Called when the user selects a charge from the LOV."
    )
    @GetMapping("/get-tax-rate")
    public ResponseEntity<?> getChargeDefaults(
            @Parameter(description = "Charge POID", required = true) @RequestParam Long chargePoid,
            @Parameter(description = "Transaction Date") @RequestParam(required = false) LocalDate transactionDate) {
        try {
            ChargeDefaultsRequestDto request = ChargeDefaultsRequestDto.builder()
                    .chargePoid(chargePoid)
                    .transactionDate(transactionDate)
                    .build();
            ChargeDefaultsResponseDto response = service.getChargeDefaults(request);
            return ApiResponse.success("Charge defaults retrieved successfully", response);
        } catch (ValidationException e) {
            log.warn("Validation failed for charge tax defaults: {}", e.getMessage());
            return error(e.getMessage(), 400);
        } catch (Exception e) {
            log.error("Failed to fetch charge tax defaults for chargePoid: {}", chargePoid, e);
            return error("Failed to fetch charge tax defaults: " + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(
            summary = "Update Local Charges",
            description = "Press for Local Charges — fetches and loads port local charges from Port Charges Master into the charges tab."
    )
    @PostMapping("/{transactionPoid}/update-local-charges")
    public ResponseEntity<?> updateLocalCharges(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
        try {
            return success("Local charges loaded successfully", service.loadLocalCharges(transactionPoid));
        } catch (ValidationException e) {
            log.warn("Validation failed while loading local charges for {}: {}", transactionPoid, e.getMessage());
            return error(e.getMessage(), 400);
        } catch (ResourceNotFoundException e) {
            return error(e.getMessage(), 404);
        } catch (Exception e) {
            log.error("Failed to load local charges for Export Manifest BL: {}", transactionPoid, e);
            return error("Failed to load local charges: " + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(
            summary = "Load Port Local Charges",
            description = "Alias for update-local-charges. Loads local charges from Port Charges Master via PROC_SHIP_BL_PAGE_SAVE_AFTER."
    )
    @PostMapping("/{transactionPoid}/load-local-charges")
    public ResponseEntity<?> loadLocalCharges(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
        return updateLocalCharges(transactionPoid);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(
            summary = "Select Bookings — Load Selected (legacy loadSelectedBookingData)",
            description = """
                    Stages GLOBAL_TEMP_BOOKING_SELECTED, then FUNC_LOAD_BOOKING_TO_BL(user, issueVesselVoyagePoid).
                    Issue Vessel Voyage POID = pVoyageVesselPoid1. Returns new BL transactionPoid; GET BL for tabs.
                    """
    )
    @PostMapping("/{issueVesselVoyagePoid}/load-booking")
    public ResponseEntity<?> loadBooking(
            @Parameter(description = "Issue Vessel Voyage POID for FUNC_LOAD_BOOKING_TO_BL", required = true)
            @PathVariable Long issueVesselVoyagePoid,
            @Valid @RequestBody LoadBookingRequest request) {
        try {
            return success("Booking data loaded successfully", service.loadBooking(issueVesselVoyagePoid, request));
        } catch (ValidationException e) {
            log.warn("Load booking validation failed for voyage {}: {}", issueVesselVoyagePoid, e.getMessage());
            return error(e.getMessage(), 400);
        } catch (Exception e) {
            log.error("Failed to load booking for voyage {}", issueVesselVoyagePoid, e);
            return error("Error loading booking: " + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(
            summary = "Load Customer Local Charges",
            description = "Loads customer-mapped export charges into the BL charges tab via PROC_SHIP_BL_CUSTOMER_AUTO."
    )
    @PostMapping("/{transactionPoid}/load-customer-local-charges")
    public ResponseEntity<?> loadCustomerLocalCharges(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
        try {
            return success("Customer local charges loaded successfully", service.loadCustomerLocalCharges(transactionPoid));
        } catch (ValidationException e) {
            log.warn("Validation failed while loading customer local charges for {}: {}", transactionPoid, e.getMessage());
            return error(e.getMessage(), 400);
        } catch (ResourceNotFoundException e) {
            return error(e.getMessage(), 404);
        } catch (Exception e) {
            log.error("Failed to load customer local charges for Export Manifest BL: {}", transactionPoid, e);
            return error("Failed to load customer local charges: " + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(
            summary = "Load Damage Clause",
            description = """
                    Legacy **Damage Clause** button: calls `PROC_SHIP_BL_DAMAGE_LOAD` (OUT cursor only),
                    then inserts `SHIP_BL_MANIFEST_CARGO_DTL` rows for the BL like ADF `damageClauseButton()`.
                    """
    )
    @RequestMapping(value = "/{transactionPoid}/damage-clause", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> loadDamageClause(
            @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
        try {
            return success("Damage clause loaded successfully", service.loadDamageClause(transactionPoid));
        } catch (ValidationException e) {
            log.warn("Validation failed while loading damage clause for {}: {}", transactionPoid, e.getMessage());
            return error(e.getMessage(), 400);
        } catch (ResourceNotFoundException e) {
            return error(e.getMessage(), 404);
        } catch (Exception e) {
            log.error("Failed to load damage clause for Export Manifest BL: {}", transactionPoid, e);
            return error("Failed to load damage clause: " + e.getMessage(), 500);
        }
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @Operation(
            summary = "Approved Manifest — Select For Invoice (Charges tab)",
            description = """
                    Legacy **selectForInvoiceAction** only (not auto-invoice on approval).
                    Validates saved export BL + **FINAL_APPROVAL_COMPLETED** for 100-104.
                    Returns drill-down for navigation: `documentId` **300-102**, `documentName` **Sales Invoice (Shipping)**, and `blPoid`.
                    SPA opens a new tab using those fields. Does **not** create invoice or post GL.
                    User needs **300-102 Create** on the invoice screen.
                    """
    )
    @PostMapping("/{transactionPoid}/select-for-invoice")
    public ResponseEntity<?> selectForInvoice(
            @Parameter(description = "Export BL transaction POID", required = true) @PathVariable Long transactionPoid) {
        try {
            return success("Select for invoice completed", service.selectForInvoice(transactionPoid));
        } catch (ValidationException e) {
            log.warn("Validation failed for select-for-invoice on {}: {}", transactionPoid, e.getMessage());
            return error(e.getMessage(), 400);
        } catch (ResourceNotFoundException e) {
            return error(e.getMessage(), 404);
        } catch (Exception e) {
            log.error("Failed select-for-invoice for Export Manifest BL: {}", transactionPoid, e);
            return error("Failed to select BL for invoice: " + e.getMessage(), 500);
        }
    }
}


