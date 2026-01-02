package com.asg.shipping.deliveryorderissuetocustomer.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.deliveryorderissuetocustomer.dto.DeliveryOrderIssueToCustomerDto;
import com.asg.shipping.deliveryorderissuetocustomer.dto.IssueDeliveryOrderRequestDto;
import com.asg.shipping.deliveryorderissuetocustomer.dto.UpdateDeliveryOrderRequestDto;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface DeliveryOrderIssueToCustomerService {

    /**
     * Search pending delivery orders from view
     */
    Map<String, Object> listDeliveryOrderIssueToCustomer(String docId, FilterRequestDto request, Pageable pageable);

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