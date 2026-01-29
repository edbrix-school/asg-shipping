package com.asg.shipping.customerinvoicechargemapmaster.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "CUSTOMER_INVOICE_PRT_MASTER")
@Getter
@Setter
public class CustomerInvoicePrtMasterEntity {

    @Id
    @Column(name = "CUSTOMER_POID")
    private Long customerPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "SEQNO")
    private Integer seqNo;

    @Column(name = "ACTIVE")
    private String active;

    @Column(name = "DELETED")
    private String deleted;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;
}
