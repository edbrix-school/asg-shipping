package com.asg.shipping.customerinvoicechargemapmaster.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class CustomerInvoicePrtDtlId {

    @Column(name = "CUSTOMER_POID")
    private Long customerPoid;

    @Column(name = "DET_ROW_ID")
    private Long detRowId;
}
