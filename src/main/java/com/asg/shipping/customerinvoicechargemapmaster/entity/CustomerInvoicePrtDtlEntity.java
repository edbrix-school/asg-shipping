package com.asg.shipping.customerinvoicechargemapmaster.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "CUSTOMER_INVOICE_PRT_DTL")
@Getter
@Setter
public class CustomerInvoicePrtDtlEntity {

    @EmbeddedId
    private CustomerInvoicePrtDtlId id;

    @Column(name = "CHARGE_POID")
    private Long chargePoid;

    @Column(name = "LINE_CHARGE_DESCRIPTION")
    private String lineChargeDescription;

    @Column(name = "VALID_UNTIL")
    private LocalDate validUntil;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;
}

