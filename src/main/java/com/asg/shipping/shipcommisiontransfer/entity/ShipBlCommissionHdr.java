package com.asg.shipping.shipcommisiontransfer.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity class for SHIP_BL_COMMISSION_HDR table
 */
@Entity
@Table(name = "SHIP_BL_COMMISSION_HDR",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_DOCRESHIPBLCOMMISSIONHDR", columnNames = {"DOC_REF"})
        })
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ShipBlCommissionHdr extends BaseEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionPoid;

    @Column(name = "GROUP_POID", nullable = false)
    private Long groupPoid;

    @Column(name = "COMPANY_POID", nullable = false)
    private Long companyPoid;

    @Column(name = "DOC_REF", length = 25, unique = true)
    private String docRef;

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

    @Column(name = "VOYAGE_TRANSACTION_POID")
    private Long voyageTransactionPoid;

    @Column(name = "REMARKS", length = 200)
    private String remarks;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "CURRENCY_EXCHANGE", precision = 20, scale = 3)
    private BigDecimal currencyExchange;

    @Column(name = "CURRENCY_CODE", length = 25)
    private String currencyCode;

    @Column(name = "FDA_TRANSACTION_POID")
    private Long fdaTransactionPoid;

    @PrePersist
    protected void onCreate() {
        if (deleted == null) {
            deleted = "N";
        }
        if (transactionDate == null) {
            transactionDate = LocalDate.now();
        }
    }
}

