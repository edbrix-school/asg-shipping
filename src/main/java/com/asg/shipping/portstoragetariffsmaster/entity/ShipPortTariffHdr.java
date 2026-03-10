package com.asg.shipping.portstoragetariffsmaster.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity class for SHIP_PORT_TARIFF_HDR table
 */
@Entity
@Table(name = "SHIP_PORT_TARIFF_HDR",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_DOCREFFSHIP_PORT__HDR", columnNames = {"DOC_REF"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipPortTariffHdr extends BaseEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "PORT_POID", nullable = false)
    private Long portPoid;

    @Column(name = "DESCRIPTION", nullable = false, length = 100)
    private String description;

    @Column(name = "TARIFF_TYPE", nullable = false, length = 50)
    private String tariffType;

    @Column(name = "PERIOD_FROM", nullable = false)
    private LocalDate periodFrom;

    @Column(name = "PERIOD_TO", nullable = false)
    private LocalDate periodTo;

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

    @Column(name = "DOC_REF", length = 25, unique = true)
    private String docRef;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @PrePersist
    protected void onCreate() {
        if (deleted == null) {
            deleted = "N";
        }
        if (transactionDate == null) {
            transactionDate = LocalDate.now();
        }
    }

  /*  @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = LocalDateTime.now();
    }*/
}