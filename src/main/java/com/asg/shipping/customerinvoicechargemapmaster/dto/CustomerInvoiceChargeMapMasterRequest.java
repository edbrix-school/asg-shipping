package com.asg.shipping.customerinvoicechargemapmaster.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CustomerInvoiceChargeMapMasterRequest {

    @NotNull
    @Positive
    private Long customerPoid;

    @NotEmpty
    @Valid
    private List<CustomerInvoiceChargeMapDetailDto> details;
}


