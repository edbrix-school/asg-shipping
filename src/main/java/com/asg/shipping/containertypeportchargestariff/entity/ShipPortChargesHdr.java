package com.asg.shipping.containertypeportchargestariff.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "SHIP_PORT_CHARGES_HDR",
        uniqueConstraints = {
                @UniqueConstraint(name = "SHIP_PORT_CHARGES_HDR_PK", columnNames = "TRANSACTION_POID"),
                @UniqueConstraint(name = "UK_DOCREFFSHIP_PORT_HDR", columnNames = "DOC_REF")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShipPortChargesHdr extends BaseEntity {

    @AuditIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @AuditIgnore
    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

    @AuditIgnore
    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "PORT_POID", nullable = false)
    private Long portPoid;

    @Column(name = "DESCRIPTION", nullable = false, length = 100)
    private String description;

    @Column(name = "PERIOD_FROM", nullable = false)
    private LocalDate periodFrom;

    @Column(name = "PERIOD_TO", nullable = false)
    private LocalDate periodTo;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "DOC_REF", length = 25)
    private String docRef;

    @Column(name = "SEQNO")
    private Integer seqNo;

    @Column(name = "CHARGE_LINE_POID")
    private Long chargeLinePoid;

    @Column(name = "CHARGE_DIVISION", length = 10)
    private String chargeDivision;
}

