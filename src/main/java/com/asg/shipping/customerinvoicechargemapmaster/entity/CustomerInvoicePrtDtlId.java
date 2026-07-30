package com.asg.shipping.customerinvoicechargemapmaster.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class CustomerInvoicePrtDtlId implements Serializable {

    @Column(name = "CUSTOMER_POID")
    private Long customerPoid;

    @Column(name = "DET_ROW_ID")
    private Long detRowId;
}
