package com.asg.shipping.customerinvoicechargemapmaster.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "CUSTOMER_INVOICE_PRT_DTL")
@Getter
@Setter
public class CustomerInvoicePrtDtlEntity extends BaseEntity {

    @AuditIgnore
    @EmbeddedId
    private CustomerInvoicePrtDtlId id;

    @Column(name = "CHARGE_POID")
    private Long chargePoid;

    @Column(name = "LINE_CHARGE_DESCRIPTION")
    private String lineChargeDescription;

    @Column(name = "VALID_UNTIL")
    private LocalDate validUntil;

}

