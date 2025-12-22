package com.asg.shipping.customerinvoicechargemapmaster.controller;

import com.asg.shipping.common.ApiResponse;
import com.asg.shipping.customerinvoicechargemapmaster.dto.CustomerInvoiceChargeMapMasterRequest;
import com.asg.shipping.customerinvoicechargemapmaster.dto.CustomerInvoiceChargeMapMasterResponse;
import com.asg.shipping.customerinvoicechargemapmaster.service.CustomerInvoiceChargeMapMasterService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customer-invoice-charge-map")
public class CustomerInvoiceChargeMapMasterController {

    private final CustomerInvoiceChargeMapMasterService service;

    /**
     * Get invoice charge mapping by customer
     */
    @GetMapping("/{customerPoid}")
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
    @PostMapping
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
    @DeleteMapping("/{customerPoid}/details/{detRowId}")
    public ResponseEntity<?> deleteDetail(
            @PathVariable @NotNull @Positive Long customerPoid,
            @PathVariable @NotNull @Positive Long detRowId,
            @RequestHeader("X-Group-Poid") Long groupPoid,
            @RequestHeader("X-User-Id") String userId
    ) {
        service.deleteDetail(customerPoid, detRowId, groupPoid, userId);

        return ApiResponse.success("Customer invoice charge detail deleted successfully"
        );
    }
}
