package com.asg.shipping.customerinvoicechargemapmaster.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.common.ApiResponse;
import com.asg.shipping.customerinvoicechargemapmaster.dto.CustomerInvoiceChargeMapMasterRequest;
import com.asg.shipping.customerinvoicechargemapmaster.dto.CustomerInvoiceChargeMapMasterResponse;
import com.asg.shipping.customerinvoicechargemapmaster.service.CustomerInvoiceChargeMapMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@RequestMapping("v1/customer-invoice-charge-map-master")
@Tag(
        name = "Customer Invoice Charge Map Master",
        description = "APIs for managing Customer Invoice Charge Mapping"
)
public class CustomerInvoiceChargeMapMasterController {

    private final LoggingService loggingService;

    private final CustomerInvoiceChargeMapMasterService service;

    /**
     * Get invoice charge mapping by customer
     */
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{customerPoid}")
    @Operation(
            summary = "Get customer invoice charge mapping",
            description = "Retrieve customer invoice charge mapping by customer POID",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved customer invoice charge mapping",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomerInvoiceChargeMapMasterResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Customer invoice charge mapping not found",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> getByCustomer(
            @Parameter(description = "Customer POID", required = true, example = "12345")
            @PathVariable @NotNull @Positive Long customerPoid
    ) {
        log.info("Getting customer invoice charge mapping with customerPoid: {}", customerPoid);
        CustomerInvoiceChargeMapMasterResponse response =
                service.getByCustomer(customerPoid, UserContext.getGroupPoid());
        loggingService.createLogSummaryEntry(
                LogDetailsEnum.VIEWED,
                UserContext.getDocumentId(),
                customerPoid.toString()
        );
        log.info("Successfully retrieved customer invoice charge mapping with customerPoid: {}", customerPoid);
        return ApiResponse.success(
                "Customer invoice charge mapping fetched successfully",
                response
        );
    }

    /**
     * Create or update customer invoice charge mapping
     */
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    @Operation(
            summary = "Create or update customer invoice charge mapping",
            description = "Create a new or update existing customer invoice charge mapping",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully saved customer invoice charge mapping",
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
    public ResponseEntity<?> saveOrUpdate(
            @Parameter(description = "Customer invoice charge mapping data", required = true)
            @Valid @RequestBody CustomerInvoiceChargeMapMasterRequest request
    ) {
        log.info("Saving/updating customer invoice charge mapping for customerPoid: {}", request.getCustomerPoid());
        service.saveOrUpdate(request, UserContext.getGroupPoid(), getCurrentUser());
        log.info("Successfully saved/updated customer invoice charge mapping for customerPoid: {}", request.getCustomerPoid());
        return ApiResponse.success(
                "Customer invoice charge mapping saved successfully"
        );
    }

    /**
     * Delete a specific charge detail
     */
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{customerPoid}/details/{detRowId}")
    @Operation(
            summary = "Delete customer invoice charge mapping detail",
            description = "Delete a specific charge detail from customer invoice charge mapping",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully deleted customer invoice charge detail",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Customer invoice charge detail not found",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> deleteDetail(
            @Parameter(description = "Customer POID", required = true, example = "12345")
            @PathVariable @NotNull @Positive Long customerPoid,
            @Parameter(description = "Detail Row ID", required = true, example = "1")
            @PathVariable @NotNull @Positive Long detRowId
    ) {
        log.info("Deleting customer invoice charge detail with customerPoid: {}, detRowId: {}", customerPoid, detRowId);
        service.deleteDetail(customerPoid, detRowId, UserContext.getGroupPoid(), getCurrentUser());
        log.info("Successfully deleted customer invoice charge detail with customerPoid: {}, detRowId: {}", customerPoid, detRowId);
        return ApiResponse.success(
                "Customer invoice charge detail deleted successfully"
        );
    }
}
