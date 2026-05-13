package com.asg.shipping.customerinvoicechargemapmaster.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CustomerInvoiceChargeMapDetailDto {
    private Long detRowId;
    private Long chargePoid;
    private String lineChargeDescription;
    private LocalDate validUntil;
    private String actionType;
}

