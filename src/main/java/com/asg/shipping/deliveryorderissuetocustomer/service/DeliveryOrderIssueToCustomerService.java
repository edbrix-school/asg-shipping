package com.asg.shipping.deliveryorderissuetocustomer.service;

import com.asg.shipping.deliveryorderissuetocustomer.dto.DeliveryOrderIssueToCustomerDto;
import com.asg.shipping.deliveryorderissuetocustomer.dto.IssueDeliveryOrderRequestDto;
import com.asg.shipping.deliveryorderissuetocustomer.dto.UpdateDeliveryOrderRequestDto;

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
}