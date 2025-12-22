package com.asg.shipping.customerinvoicechargemapmaster.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CustomerInvoiceChargeMapMasterResponse {
    private Long customerPoid;
    private String customerName;
    private List<CustomerInvoiceChargeMapDetailDto> details;
}

