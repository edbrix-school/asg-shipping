package com.asg.shipping.deliveryorderissuetocustomer.service;

import com.asg.shipping.deliveryorderissuetocustomer.dto.*;
import com.asg.shipping.deliveryorderissuetocustomer.enums.ButtonType;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface DeliveryOrderIssueToCustomerService {
    DeliveryOrderIssueToCustomerDto getDeliveryOrderIssueToCustomer(Long id);

    void issueDeliveryOrder(Long id, IssueDeliveryOrderRequestDto request);

    Long updateDeliveryOrder(Long id, UpdateDeliveryOrderRequestDto request);

    byte[] print(Long transactionPoid, @Valid IssueDeliveryOrderRequestDto requestDto, ButtonType buttonType) throws Exception;

    ValidateDocumentDto validateDocument(Long id, @Valid IssueDeliveryOrderRequestDto requestDto);
    
    Map<String, Object> searchDeliveryOrders(String documentId, com.asg.common.lib.dto.FilterRequestDto filters, Pageable pageable);
}