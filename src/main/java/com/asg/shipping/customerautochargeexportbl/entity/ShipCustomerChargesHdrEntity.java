package com.asg.shipping.customerautochargeexportbl.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(name = "SHIP_CUSTOMER_CHARGES_HDR")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipCustomerChargesHdrEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

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

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "DOC_REF", length = 25, unique = true)
    private String docRef;

    @Column(name = "SEQNO")
    private Integer seqNo;
}
