package com.asg.shipping.deliveryorderissuetocustomer.service;

import com.asg.shipping.deliveryorderissuetocustomer.dto.*;
import com.asg.shipping.deliveryorderissuetocustomer.enums.ButtonType;
import jakarta.validation.Valid;

public interface DeliveryOrderIssueToCustomerService {
    /**
     * Get delivery order by BL transaction POID
     */
    DeliveryOrderIssueToCustomerDto getDeliveryOrderIssueToCustomer(Long id);

    /**
     * Issue delivery order to customer
     */
    void issueDeliveryOrder(Long id, IssueDeliveryOrderRequestDto request);

    /**
     * Update delivery order details
     */
    DeliveryOrderIssueToCustomerDto updateDeliveryOrder(Long id, UpdateDeliveryOrderRequestDto request);

    byte[] print(Long transactionPoid, @Valid IssueDeliveryOrderRequestDto requestDto, ButtonType buttonType) throws Exception;

    ValidateDocumentDto validateDocument(Long id, @Valid IssueDeliveryOrderRequestDto requestDto);
}