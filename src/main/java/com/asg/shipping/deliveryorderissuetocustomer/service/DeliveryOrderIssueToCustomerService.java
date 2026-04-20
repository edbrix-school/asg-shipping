package com.asg.shipping.deliveryorderissuetocustomer.service;

import com.asg.shipping.deliveryorderissuetocustomer.dto.*;
import com.asg.shipping.deliveryorderissuetocustomer.enums.ButtonType;
import jakarta.validation.Valid;

public interface DeliveryOrderIssueToCustomerService {
    DeliveryOrderIssueToCustomerDto getDeliveryOrderIssueToCustomer(Long id);

    void issueDeliveryOrder(Long id, IssueDeliveryOrderRequestDto request);

    Long updateDeliveryOrder(Long id, UpdateDeliveryOrderRequestDto request);

    byte[] print(Long transactionPoid, @Valid IssueDeliveryOrderRequestDto requestDto, ButtonType buttonType) throws Exception;

    ValidateDocumentDto validateDocument(Long id, @Valid IssueDeliveryOrderRequestDto requestDto);
}