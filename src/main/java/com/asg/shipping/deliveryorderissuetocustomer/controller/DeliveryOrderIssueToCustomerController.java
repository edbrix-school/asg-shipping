package com.asg.shipping.deliveryorderissuetocustomer.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.shipping.deliveryorderissuetocustomer.dto.DeliveryOrderIssueToCustomerDto;
import com.asg.shipping.deliveryorderissuetocustomer.dto.DeliveryOrderIssueToCustomerPrintRequest;
import com.asg.shipping.deliveryorderissuetocustomer.dto.IssueDeliveryOrderRequestDto;
import com.asg.shipping.deliveryorderissuetocustomer.dto.UpdateDeliveryOrderRequestDto;
import com.asg.shipping.deliveryorderissuetocustomer.service.DeliveryOrderIssueToCustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
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

    /**
     * Get delivery order by BL transaction POID
     * GET /v1/delivery-order-issue-to-customer/{id}
     */
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{id}")
    public ResponseEntity<?> getDeliveryOrderIssueToCustomer(@PathVariable Long id) {
        log.info("Get request for Delivery Order Issue To Customer with id: {}", id);
        DeliveryOrderIssueToCustomerDto dto = deliveryOrderIssueToCustomerService.getDeliveryOrderIssueToCustomer(id);
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
    public ResponseEntity<?> updateDeliveryOrder(@PathVariable Long id,
                                                 @Valid @RequestBody UpdateDeliveryOrderRequestDto request) {
        log.info("Update request for Delivery Order Issue To Customer with id: {}", id);
        DeliveryOrderIssueToCustomerDto dto = deliveryOrderIssueToCustomerService.updateDeliveryOrder(id, request);
        return success("Delivery order updated successfully", dto);
    }

//    @AllowedAction(UserRolesRightsEnum.PRINT)
//    @PostMapping(
//            value = "/validate-Document/{transactionPoid}",
//            produces = MediaType.MULTIPART_MIXED_VALUE
//    )
//    public ResponseEntity<?> print(
//            @PathVariable Long transactionPoid,
//            @RequestBody IssueDeliveryOrderRequestDto request
//    ) {
//        try {
//           , byte[] pdfs =
//                    deliveryOrderIssueToCustomerService.validateDocument(transactionPoid, request);
//
//            if (pdfs.isEmpty()) {
//                return ResponseEntity.noContent().build();
//            }
//
//            MultipartBodyBuilder builder = new MultipartBodyBuilder();
//
//            pdfs.forEach((name, bytes) ->
//                    builder.part(name, bytes)
//                            .contentType(MediaType.APPLICATION_PDF)
//                            .header(
//                                    HttpHeaders.CONTENT_DISPOSITION,
//                                    "attachment; filename=" + name + "-" + transactionPoid + ".pdf"
//                            )
//            );
//
//            return ResponseEntity.ok()
//                    .contentType(MediaType.MULTIPART_MIXED)
//                    .body(builder.build());
//
//        } catch (Exception e) {
//            log.error("Failed to generate PDFs", e);
//            return error("Failed to generate PDFs: " + e.getMessage(), 500);
//        }
//    }

}