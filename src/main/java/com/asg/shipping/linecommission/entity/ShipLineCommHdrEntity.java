package com.asg.shipping.linecommission.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "SHIP_LINE_COMM_HDR")
@Getter
@Setter
public class ShipLineCommHdrEntity extends BaseEntity {

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

}


