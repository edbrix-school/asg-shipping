package com.asg.shipping.containertypeportchargestariff.entity;

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
public class ShipPortChargesHdr {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "SHIP_PORT_CHARGES_HDR_SEQ_GEN"
    )
    @SequenceGenerator(
            name = "SHIP_PORT_CHARGES_HDR_SEQ_GEN",
            sequenceName = "SHIP_PORT_CHARGES_HDR_SEQ",
            allocationSize = 1
    )
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

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

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

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

