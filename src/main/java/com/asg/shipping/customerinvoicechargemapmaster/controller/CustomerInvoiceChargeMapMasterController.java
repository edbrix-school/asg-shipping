package com.asg.shipping.customerinvoicechargemapmaster.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.shipping.common.ApiResponse;
import com.asg.shipping.customerinvoicechargemapmaster.dto.CustomerInvoiceChargeMapMasterRequest;
import com.asg.shipping.customerinvoicechargemapmaster.dto.CustomerInvoiceChargeMapMasterResponse;
import com.asg.shipping.customerinvoicechargemapmaster.service.CustomerInvoiceChargeMapMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@RequestMapping("/v1/customer-invoice-charge-map")
@Tag(
        name = "Customer Invoice Charge Map Master",
        description = "APIs for managing Customer Invoice Charge Mapping"
)
public class CustomerInvoiceChargeMapMasterController {

    private final CustomerInvoiceChargeMapMasterService service;

    /**
     * Get invoice charge mapping by customer
     */
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{customerPoid}")
    @Operation(summary = "Get customer invoice charge mapping")
    public ResponseEntity<?> getByCustomer(
            @PathVariable @NotNull @Positive Long customerPoid,
            @RequestHeader("X-Group-Poid") Long groupPoid
    ) {
        CustomerInvoiceChargeMapMasterResponse response =
                service.getByCustomer(customerPoid, groupPoid);

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
    @Operation(summary = "Create or update customer invoice charge mapping")
    public ResponseEntity<?> saveOrUpdate(
            @Valid @RequestBody CustomerInvoiceChargeMapMasterRequest request,
            @RequestHeader("X-Group-Poid") Long groupPoid,
            @RequestHeader("X-User-Id") String userId
    ) {
        service.saveOrUpdate(request, groupPoid, userId);

        return ApiResponse.success(
                "Customer invoice charge mapping saved successfully"
        );
    }


    /**
     * Delete a specific charge detail
     */
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{customerPoid}/details/{detRowId}")
    @Operation(summary = "Delete customer invoice charge mapping detail")
    public ResponseEntity<?> deleteDetail(
            @PathVariable @NotNull @Positive Long customerPoid,
            @PathVariable @NotNull @Positive Long detRowId,
            @RequestHeader("X-Group-Poid") Long groupPoid,
            @RequestHeader("X-User-Id") String userId
    ) {
        service.deleteDetail(customerPoid, detRowId, groupPoid, userId);

        return ApiResponse.success(
                "Customer invoice charge detail deleted successfully"
        );
    }
}
