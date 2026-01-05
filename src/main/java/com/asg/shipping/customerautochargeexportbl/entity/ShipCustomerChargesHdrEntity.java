package com.asg.shipping.customerautochargeexportbl.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.Date;

@Entity
@Table(name = "SHIP_CUSTOMER_CHARGES_HDR")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipCustomerChargesHdrEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Column(name = "TRANSACTION_DATE")
    @Temporal(TemporalType.DATE)
    private Date transactionDate;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "CUSTOMER_POID", nullable = false)
    private Long customerPoid;

    @Column(name = "DESCRIPTION", length = 100, nullable = false)
    private String description;

    @Column(name = "PERIOD_FROM", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date periodFrom;

    @Column(name = "PERIOD_TO", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date periodTo;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "DOC_REF", length = 25, unique = true)
    private String docRef;

    @Column(name = "SEQNO")
    private Integer seqNo;
}
