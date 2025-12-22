package com.asg.shipping.customerinvoicechargemapmaster.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CustomerInvoiceChargeMapMasterRequest {
    private Long customerPoid;
    private List<CustomerInvoiceChargeMapDetailDto> details;
}

