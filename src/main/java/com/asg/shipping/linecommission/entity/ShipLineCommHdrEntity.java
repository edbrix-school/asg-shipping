package com.asg.shipping.linecommission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "SHIP_LINE_COMM_HDR")
@Getter
@Setter
public class ShipLineCommHdrEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionPoid;

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "LINE_POID")
    private Long linePoid;

    @Column(name = "PERIOD_FROM", nullable = false)
    private LocalDate periodFrom;

    @Column(name = "PERIOD_TO", nullable = false)
    private LocalDate periodTo;

    @Column(name = "RENEWAL_DATE")
    private LocalDate renewalDate;

    @Column(name = "CURRENCY_POID")
    private Long currencyPoid;

    @Column(name = "DOC_REF")
    private String docRef;

    @Column(name = "DELETED")
    private String deleted;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "REMARKS")
    private String remarks;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;
}


