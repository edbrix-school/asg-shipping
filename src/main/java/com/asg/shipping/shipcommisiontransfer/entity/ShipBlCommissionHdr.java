package com.asg.shipping.shipcommisiontransfer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.asg.common.lib.security.util.UserContext.getUserName;

/**
 * Entity class for SHIP_BL_COMMISSION_HDR table
 */
@Entity
@Table(name = "SHIP_BL_COMMISSION_HDR",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_DOCRESHIPBLCOMMISSIONHDR", columnNames = {"DOC_REF"})
        })
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ShipBlCommissionHdr {

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

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        if (createdBy == null) {
            createdBy = getUserName();
        }
        if (deleted == null) {
            deleted = "N";
        }
        if (transactionDate == null) {
            transactionDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = LocalDateTime.now();
        lastModifiedBy = getUserName();
    }
}

