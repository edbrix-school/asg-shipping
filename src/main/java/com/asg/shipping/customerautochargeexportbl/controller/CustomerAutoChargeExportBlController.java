package com.asg.shipping.customerautochargeexportbl.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.DocumenResponsetDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.customerautochargeexportbl.dto.CustomerAutoChargeExportBLCreateDTO;
import com.asg.shipping.customerautochargeexportbl.dto.CustomerAutoChargeExportBLDto;
import com.asg.shipping.customerautochargeexportbl.dto.CustomerAutoChargeExportBLUpdateDTO;
import com.asg.shipping.customerautochargeexportbl.service.CustomerAutoChargeExportBlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@RestController
@RequestMapping("v1/customer-auto-charge-export-bl")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "customer-auto-charge-export-bl-controller", description = "Manage Customer Auto Charge Export BL records")
public class CustomerAutoChargeExportBlController {

    private final CustomerAutoChargeExportBlService service;
    private final LoggingService loggingService;


    @AllowedAction(UserRolesRightsEnum.CREATE)
    @Operation(
            summary = "Create Customer Auto Charge Export BL",
            description = """
                    Create a new Customer Auto Charge Export BL record.
                    
                    ### Business Rules
                    - **Customer POID** is mandatory
                    - **Description** is mandatory and cannot be blank
                    - **Period From Date** must be less than or equal to **Period To Date**
                    - **Document Reference** must be unique if provided
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer Auto Charge Export BL created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or duplicate document reference"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody CustomerAutoChargeExportBLCreateDTO createDTO
    ) {
        try {
            CustomerAutoChargeExportBLDto response = service.createCustomerAutoChargeExportBL(createDTO);
            return success("Customer Auto Charge Export BL created successfully", response);
        } catch (ValidationException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to create Customer Auto Charge Export BL: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(
            summary = "Update Customer Auto Charge Export BL",
            description = """
                    Update an existing Customer Auto Charge Export BL record.
                    
                    ### Business Rules
                    - Cannot update deleted records
                    - **Period From Date** must be less than or equal to **Period To Date**
                    - **Document Reference** must be unique if provided
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer Auto Charge Export BL updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Customer Auto Charge Export BL not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody CustomerAutoChargeExportBLUpdateDTO updateDTO
    ) {
        try {
            CustomerAutoChargeExportBLDto response = service.updateCustomerAutoChargeExportBL(id, updateDTO);
            return success("Customer Auto Charge Export BL updated successfully", response);
        } catch (ValidationException | IllegalArgumentException | IllegalStateException e) {
            return badRequest(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to update Customer Auto Charge Export BL: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "Get Customer Auto Charge Export BL by ID",
            description = "Retrieve a single Customer Auto Charge Export BL record by its Transaction POID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer Auto Charge Export BL retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Customer Auto Charge Export BL not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long id
    ) {

            CustomerAutoChargeExportBLDto response = service.getCustomerAutoChargeExportBL(id);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), id.toString());
        return success("Customer Auto Charge Export BL retrieved successfully", response);

    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @Operation(
            summary = "Delete Customer Auto Charge Export BL",
            description = "Soft delete a Customer Auto Charge Export BL record and its associated detail records."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer Auto Charge Export BL deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Customer Auto Charge Export BL not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody DeleteReasonDto deleteReasonDto
            ) {
        try {
            service.deleteCustomerAutoChargeExportBL(id,deleteReasonDto);
            return success("Customer Auto Charge Export BL deleted successfully", null);
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to delete Customer Auto Charge Export BL: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "List Customer Auto Charge Export BLs",
            description = """
                    Fetch Customer Auto Charge Export BL records using filters and pagination.
                    
                    Valid `searchField` values: DESCRIPTION, TRANSACTION_POID, DOC_REF, CUSTOMER_POID
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    examples = @ExampleObject(
                            name = "Customer Auto Charge Export BL Filters",
                            value = """
                                    {
                                      "operator": "AND",
                                      "isDeleted": "N",
                                      "filters": [
                                        {
                                          "searchField": "DESCRIPTION",
                                          "searchValue": "Auto Charge"
                                        },
                                        {
                                          "searchField": "CUSTOMER_POID",
                                          "searchValue": "1001"
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer Auto Charge Export BL list retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/list")
    public ResponseEntity<?> list(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters
    ) {
        try {
            Map<String, Object> response = service.list(filters, pageable);
            return success("Customer Auto Charge Export BL list retrieved successfully", response);
        } catch (Exception e) {
            return internalServerError("Failed to retrieve Customer Auto Charge Export BL list: " + e.getMessage());
        }
    }
}
