package com.asg.shipping.shippingmanifestcorrector.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.shippingmanifestcorrector.dto.*;
import com.asg.shipping.shippingmanifestcorrector.service.ManifestCorrectorService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;

/**
 * REST Controller for Shipping Manifest Corrector operations
 */
@RestController
@RequestMapping("/v1/shipping-manifest-corrector")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "manifest-corrector-controller",
        description = "Manage Shipping Manifest Corrector (Add/Update/Refund/Reprint) records (DocId: 100-143)"
)
public class ManifestCorrectorController {

    private final ManifestCorrectorService service;
    private final LoggingService loggingService;

    /**
     * Search Shipping Manifest Corrector records
     */
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "Search Shipping Manifest Corrector Records (DocId: 100-143)",
            description = "Search Shipping Manifest Corrector records with filters and pagination. " +
                    "Supports filtering by DOC_REF, BL_NUMBER, transaction date, BL type, issue type, and reprint flags.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Records retrieved successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid filter parameters"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @PostMapping("/search")
    public ResponseEntity<?> searchManifestCorrector(

            @Parameter(description = "Start date for filtering")
            @RequestParam(required = false) LocalDate startDate,

            @Parameter(description = "End date for filtering")
            @RequestParam(required = false) LocalDate endDate,
            @RequestBody(required = false) FilterRequestDto request,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field and direction (e.g., 'transactionDate,asc')")

            @RequestParam(required = false) String sort) {

        try {
            log.info("Search request for Shipping Manifest Corrector with page: {}, size: {}, sort: {}", page, size, sort);

            Pageable pageable = createPageable(page, size, sort);

            Map<String, Object> result = service.searchManifestCorrector(
                    UserContext.getDocumentId(),
                    request,
                    startDate,
                    endDate,
                    pageable
            );

            return success("Shipping Manifest Corrector records retrieved successfully", result);
        } catch (Exception e) {
            return internalServerError("Error searching Shipping Manifest Corrector records: " + e.getMessage());
        }
    }

    /**
     * Get Shipping Manifest Corrector by ID
     */
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "Get Shipping Manifest Corrector by ID (DocId: 100-143)",
            description = "Retrieve a Shipping Manifest Corrector record with all charges and container details.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Record retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Record not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/{transactionPoid}")
    public ResponseEntity<?> getManifestCorrector(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long transactionPoid) {
        try {
            log.info("Get request for Shipping Manifest Corrector with id: {}", transactionPoid);
            ManifestCorrectorDto dto = service.getManifestCorrectorById(transactionPoid);
            loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
            return success("Shipping Manifest Corrector retrieved successfully", dto);
        } catch (Exception e) {
            return internalServerError("Error fetching Shipping Manifest Corrector: " + e.getMessage());
        }
    }

    /**
     * Create Shipping Manifest Corrector record
     */
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @Operation(
            summary = "Create Shipping Manifest Corrector (DocId: 100-143)",
            description = "Create a new Shipping Manifest Corrector record for add/update/refund/reprint operations. " +
                    "Business Rules: BL number is required. Only one reprint flag can be 'Y' at a time. " +
                    "If DEM_REFUND = 'Y', demurrage payment type and customer are required.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Record created successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Provide details for Shipping Manifest Corrector header, charges, and containers.",
            content = @Content(
                    schema = @Schema(implementation = ManifestCorrectorCreateDTO.class),
                    examples = @ExampleObject(
                            name = "Manifest Corrector Create Example",
                            value = """
                                    {
                                      "transactionDate": "2024-01-15",
                                      "blNumber": "12345",
                                      "blReprint": "Y",
                                      "doReprint": "N",
                                      "containerReprint": "N",
                                      "returnReprint": "N",
                                      "demRefund": "N",
                                      "issueType": "REPRINT",
                                      "blType": "IMPORT",
                                      "chargesDetails": [],
                                      "containerDetails": []
                                    }
                                    """
                    )
            )
    )
    @PostMapping("/create")
    public ResponseEntity<?> createManifestCorrector(
            @Valid @RequestBody ManifestCorrectorCreateDTO createDTO) {
        try {
            log.info("Create request for Shipping Manifest Corrector");
            ManifestCorrectorDto dto = service.createManifestCorrector(createDTO);
            return success("Shipping Manifest Corrector created successfully", dto);
        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Error creating Shipping Manifest Corrector: " + ex.getMessage());
        }
    }

    /**
     * Update Shipping Manifest Corrector record
     */
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(
            summary = "Update Shipping Manifest Corrector (DocId: 100-143)",
            description = "Update an existing Shipping Manifest Corrector record. " +
                    "If reprint flags are changed, charges/containers will be reloaded automatically.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Record updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "404", description = "Record not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Provide updated details for Shipping Manifest Corrector.",
            content = @Content(
                    schema = @Schema(implementation = ManifestCorrectorUpdateDTO.class)
            )
    )
    @PutMapping("/{transactionPoid}")
    public ResponseEntity<?> updateManifestCorrector(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long transactionPoid,
            @Valid @RequestBody ManifestCorrectorUpdateDTO updateDTO) {
        try {
            log.info("Update request for Shipping Manifest Corrector with id: {}", transactionPoid);
            ManifestCorrectorDto dto = service.updateManifestCorrector(transactionPoid, updateDTO);
            return success("Shipping Manifest Corrector updated successfully", dto);
        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Error updating Shipping Manifest Corrector: " + ex.getMessage());
        }
    }

    /**
     * Delete Shipping Manifest Corrector record
     */
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @Operation(
            summary = "Delete Shipping Manifest Corrector (DocId: 100-143)",
            description = "Soft delete a Shipping Manifest Corrector record.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Record deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Record not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @DeleteMapping("/{transactionPoid}")
    public ResponseEntity<?> deleteManifestCorrector(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long transactionPoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {
        try {
            log.info("Delete request for Shipping Manifest Corrector with id: {}", transactionPoid);
            service.deleteManifestCorrector(transactionPoid, deleteReasonDto);
            return success("Shipping Manifest Corrector deleted successfully");
        } catch (Exception e) {
            return internalServerError("Error deleting Shipping Manifest Corrector: " + e.getMessage());
        }
    }

    /**
     * Load demurrage refund charges
     */
//    Demurrage Refund functionality has been dropped in the latest development

//    @AllowedAction(UserRolesRightsEnum.VIEW)
//    @Operation(
//            summary = "Load Demurrage Refund Charges (DocId: 100-143)",
//            description = "Load demurrage refund charges for a BL using PROC_SHIP_BL_REPRINT_DEM_LOAD stored procedure.",
//            responses = {
//                    @ApiResponse(responseCode = "200", description = "Charges loaded successfully"),
//                    @ApiResponse(responseCode = "400", description = "BL POID is required"),
//                    @ApiResponse(responseCode = "404", description = "BL not found"),
//                    @ApiResponse(responseCode = "500", description = "Internal server error")
//            }
//    )
//    @io.swagger.v3.oas.annotations.parameters.RequestBody(
//            required = true,
//            description = "Provide transaction POID and BL POID to load demurrage refund charges.",
//            content = @Content(
//                    schema = @Schema(implementation = LoadDemurrageRefundRequest.class)
//            )
//    )
//    @PostMapping("/{transactionPoid}/load-demurrage-refund")
//    public ResponseEntity<?> loadDemurrageRefundCharges(
//            @Parameter(description = "Transaction POID", required = true, example = "12345")
//            @PathVariable Long transactionPoid,
//            @Valid @RequestBody LoadDemurrageRefundRequest request) {
//        try {
//            log.info("Load demurrage refund charges request for transaction: {}, BL: {}", transactionPoid, request.getBlPoid());
//            List<ManifestCorrectorChargeDtlDto> charges = service.loadDemurrageRefundCharges(transactionPoid, request.getBlPoid());
//            return success("Demurrage refund charges loaded successfully", charges);
//        } catch (ValidationException ex) {
//            return badRequest(ex.getMessage());
//        } catch (Exception ex) {
//            return internalServerError("Error loading demurrage refund charges: " + ex.getMessage());
//        }
//    }

    /**
     * Validate refund amounts
     */
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "Validate Refund Amounts (DocId: 100-143)",
            description = "Validate reverse payable and income amounts for a container using PROC_SHIP_BL_REPRINT_AMT_VAL stored procedure. " +
                    "Validates that sum of reverse payable and income <= collected amount.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Validation completed"),
                    @ApiResponse(responseCode = "400", description = "Validation failed or invalid parameters"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Provide BL POID, container number, and refund amounts for validation.",
            content = @Content(
                    schema = @Schema(implementation = ValidateRefundAmountRequest.class)
            )
    )
    @PostMapping("/validate-refund-amounts")
    public ResponseEntity<?> validateRefundAmounts(
            @Valid @RequestBody ValidateRefundAmountRequest request) {
        try {
            log.info("Validate refund amounts request for BL: {}, Container: {}", request.getBlPoid(), request.getContainerNumber());
            ValidateRefundAmountResponse response = service.validateRefundAmounts(request);
            return success("Refund amount validation completed", response);
        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Error validating refund amounts: " + ex.getMessage());
        }
    }

    /**
     * Auto-fill DO reprint charges
     */
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "Auto-fill DO Reprint Charges (DocId: 100-143)",
            description = "Auto-fill charges for DO reprint based on BL number. Loads charges with CHARGE_TYPE_APPLICABLE='REPRINTIMP' and CHARGE_APPLICABLE='PERBL'.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Charges loaded successfully"),
                    @ApiResponse(responseCode = "400", description = "BL number is required"),
                    @ApiResponse(responseCode = "404", description = "BL not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @PostMapping("/auto-fill/do-reprint/{blNumber}")
    public ResponseEntity<?> autoFillDoReprint(
            @Parameter(description = "BL Number", required = true, example = "12345")
            @PathVariable String blNumber) {
        try {
            log.info("Auto-fill DO reprint charges for BL: {}", blNumber);
            List<ManifestCorrectorChargeDtlDto> charges = service.autoFillDoReprint(blNumber);
            return success("DO reprint charges loaded successfully", charges);
        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Error loading DO reprint charges: " + ex.getMessage());
        }
    }

    /**
     * Auto-fill container reprint charges and containers
     */
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "Auto-fill Container Reprint (DocId: 100-143)",
            description = "Auto-fill containers and charges for container reprint based on BL number. Loads containers from manifest and charges with CHARGE_APPLICABLE='PERQUENTITY'.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Container reprint data loaded successfully"),
                    @ApiResponse(responseCode = "400", description = "BL number is required"),
                    @ApiResponse(responseCode = "404", description = "BL not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @PostMapping("/auto-fill/container-reprint/{blNumber}")
    public ResponseEntity<?> autoFillContainerReprint(
            @Parameter(description = "BL Number", required = true, example = "12345")
            @PathVariable String blNumber) {
        try {
            log.info("Auto-fill container reprint for BL: {}", blNumber);
            ContainerReprintResponse result = service.autoFillContainerReprint(blNumber);
            return success("Container reprint data loaded successfully", result);
        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Error loading container reprint data: " + ex.getMessage());
        }
    }

    /**
     * Auto-fill BL reprint charges
     */
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "Auto-fill BL Reprint Charges (DocId: 100-143)",
            description = "Auto-fill charges for BL reprint based on BL number. Loads charges with CHARGE_TYPE_APPLICABLE='REPRINTEXP' and CHARGE_APPLICABLE='PERBL'.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "BL reprint charges loaded successfully"),
                    @ApiResponse(responseCode = "400", description = "BL number is required"),
                    @ApiResponse(responseCode = "404", description = "BL not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @PostMapping("/auto-fill/bl-reprint/{blNumber}")
    public ResponseEntity<?> autoFillBlReprint(
            @Parameter(description = "BL Number", required = true, example = "12345")
            @PathVariable String blNumber) {
        try {
            log.info("Auto-fill BL reprint charges for BL: {}", blNumber);
            List<ManifestCorrectorChargeDtlDto> charges = service.autoFillBlReprint(blNumber);
            return success("BL reprint charges loaded successfully", charges);
        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Error loading BL reprint charges: " + ex.getMessage());
        }
    }

    /**
     * Auto-fill DEM refund charges
     */
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "Auto-fill DEM Refund Charges (DocId: 100-143)",
            description = "Auto-fill demurrage refund charges based on BL number using stored procedure PROC_SHIP_BL_REPRINT_DEM_LOAD.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "DEM refund charges loaded successfully"),
                    @ApiResponse(responseCode = "400", description = "BL number is required"),
                    @ApiResponse(responseCode = "404", description = "BL not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @PostMapping("/auto-fill/dem-refund/{blNumber}")
    public ResponseEntity<?> autoFillDemRefund(
            @Parameter(description = "BL Number", required = true, example = "12345")
            @PathVariable String blNumber) {
        try {
            log.info("Auto-fill DEM refund charges for BL: {}", blNumber);
            List<ManifestCorrectorChargeDtlDto> charges = service.autoFillDemRefund(blNumber);
            return success("DEM refund charges loaded successfully", charges);
        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Error loading DEM refund charges: " + ex.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "BL After Browse Auto-population (DocId: 100-143)",
            description = "Auto-populate Shipping Manifest Corrector header fields from BL number using PROC_LOV_AFTER_BRWS_100_143.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "BL details loaded successfully"),
                    @ApiResponse(responseCode = "400", description = "BL number is required"),
                    @ApiResponse(responseCode = "404", description = "BL not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @PostMapping("/auto-fill/bl-after-browse/{blNumber}")
    public ResponseEntity<?> autoPopulateFromBlBrowse(
            @Parameter(description = "BL Number", required = true, example = "12345")
            @PathVariable String blNumber,
            @RequestBody(required = false) ManifestCorrectorBlAutoPopulateRequest request) {
        try {
            log.info("BL after browse auto-population request for BL: {}", blNumber);
            ManifestCorrectorBlAutoPopulateDto response = service.autoPopulateFromBlBrowse(blNumber, request);
            if (response == null) {
                return success("No BL details found", Collections.emptyList());
            }
            return success("BL details loaded successfully", response);
        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Error loading BL details: " + ex.getMessage());
        }
    }

    private Pageable createPageable(int page, int size, String sort) {

        String sortField = "TRANSACTION_DATE";
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
     * Maps frontend sort field names to SHIP_BL_REPRINT_HDR table columns
     */
    private String mapSortFieldToColumn(String sortField) {

        if (sortField == null || sortField.isBlank()) {
            return "TRANSACTION_DATE";
        }

        String normalized = sortField
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toUpperCase();

        return switch (normalized) {

            case "TRANSACTION_POID" -> "TRANSACTION_POID";
            case "COMPANY_POID" -> "COMPANY_POID";
            case "CREATED_DATE" -> "CREATED_DATE";
            case "LASTMODIFIED_DATE" -> "LASTMODIFIED_DATE";
            case "DOC_REF" -> "DOC_REF";
            case "BL_NUMBER" -> "BL_NUMBER";
            case "BL_TYPE" -> "BL_TYPE";
            case "DELETED" -> "DELETED";
            case "ISSUE_TYPE" -> "ISSUE_TYPE";
            case "CREATED_BY" -> "CREATED_BY";
            case "LASTMODIFIED_BY" -> "LASTMODIFIED_BY";

            default -> "TRANSACTION_DATE";
        };
    }

}
