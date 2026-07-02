package com.asg.shipping.deliveryorderissuetocustomer.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.deliveryorderissuetocustomer.dto.*;
import com.asg.shipping.deliveryorderissuetocustomer.enums.ButtonType;
import com.asg.shipping.deliveryorderissuetocustomer.service.DeliveryOrderIssueToCustomerService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.error;
import static com.asg.common.lib.dto.response.ApiResponse.success;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/delivery-order-issue-to-customer")
public class DeliveryOrderIssueToCustomerController {

    private final DeliveryOrderIssueToCustomerService deliveryOrderIssueToCustomerService;
    private final LoggingService loggingService;

    /**
     * Get delivery order by BL transaction POID
     * GET /v1/delivery-order-issue-to-customer/{id}
     */
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{id}")
    public ResponseEntity<?> getDeliveryOrderIssueToCustomer(@PathVariable Long id) {
        log.info("Get request for Delivery Order Issue To Customer with id: {}", id);
        DeliveryOrderIssueToCustomerDto dto = deliveryOrderIssueToCustomerService.getDeliveryOrderIssueToCustomer(id);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), id.toString());
        return success("Delivery order retrieved successfully", dto);
    }

    /**
     * Issue delivery order to customer
     * POST /v1/delivery-order-issue-to-customer/{id}/issue
     */
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping("/{id}/issue")
    public ResponseEntity<?> issueDeliveryOrder(@PathVariable Long id,
                                                @Valid @RequestBody IssueDeliveryOrderRequestDto request) {
        log.info("Issue delivery order request for BL transaction: {}", id);
        deliveryOrderIssueToCustomerService.issueDeliveryOrder(id, request);
        return success("Delivery order issued successfully");
    }

    /**
     * Update delivery order details
     * PUT /v1/delivery-order-issue-to-customer/{id}
     */
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDeliveryOrder(@PathVariable @NotNull Long id,
                                                 @Valid @RequestBody UpdateDeliveryOrderRequestDto request) {
        log.info("Update request for Delivery Order Issue To Customer with id: {}", id);
        Long transactionPoid = deliveryOrderIssueToCustomerService.updateDeliveryOrder(id, request);
        return success("Delivery order updated successfully", transactionPoid);
    }


    @AllowedAction(UserRolesRightsEnum.PRINT)
    @PostMapping("/print/{transactionPoid}")
    public ResponseEntity<?> print(@Parameter(description = "Transaction POID", example = "12345")
                                   @PathVariable Long transactionPoid,
                                   @Valid @RequestBody IssueDeliveryOrderRequestDto requestDto, @RequestParam ButtonType buttonType) {
        try {
            byte[] pdf = deliveryOrderIssueToCustomerService.print(transactionPoid, requestDto, buttonType);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=delivery-order-issue-to-customer-" + buttonType.name().toLowerCase() + "-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate PDF for delivery order issue to customer: {}", transactionPoid, e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }

    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/validate-document/{id}")
    public ResponseEntity<?> validateDocument(@PathVariable Long id, @Valid @RequestBody IssueDeliveryOrderRequestDto requestDto) {
        ValidateDocumentDto dto = deliveryOrderIssueToCustomerService.validateDocument(id, requestDto);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), id.toString());
        return success("Delivery order validated successfully", dto);
    }


    /**
     * Search delivery orders with pagination and filtering
     * POST /v1/delivery-order-issue-to-customer/list
     */
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> searchDeliveryOrders(
            @RequestBody(required = false) com.asg.common.lib.dto.FilterRequestDto filters,
            @ParameterObject Pageable pageable) {
        log.info("Searching delivery orders with filters: {}", filters);
        Map<String, Object> result = deliveryOrderIssueToCustomerService.searchDeliveryOrders(
                UserContext.getDocumentId(), filters, pageable);
        return success("Delivery orders retrieved successfully", result);
    }

}