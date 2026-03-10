package com.asg.shipping.customerinvoicechargemapmaster.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "CUSTOMER_INVOICE_PRT_MASTER")
@Getter
@Setter
public class CustomerInvoicePrtMasterEntity extends BaseEntity {

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
}
