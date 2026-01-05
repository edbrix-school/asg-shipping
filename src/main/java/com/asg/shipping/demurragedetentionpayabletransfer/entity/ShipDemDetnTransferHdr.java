package com.asg.shipping.demurragedetentionpayabletransfer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity class for SHIP_DEM_DETN_TRANSFER_HDR table
 */
@Entity
@Table(name = "SHIP_DEM_DETN_TRANSFER_HDR",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_SHIP_DEM_DETN_TRANSFER_HDR", columnNames = {"DOC_REF"})
        })
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ShipDemDetnTransferHdr {

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

    @Column(name = "LINE_POID")
    private Long linePoid;

    @Column(name = "BL_TYPE", length = 25)
    private String blType;

    @Column(name = "EMPTY_FROM_DATE")
    private LocalDate emptyFromDate;

    @Column(name = "EMPTY_TO_DATE")
    private LocalDate emptyToDate;

    @Column(name = "PAYABLE_GL_POID", nullable = false)
    private Long payableGlPoid;

    @Column(name = "INCOME_GL_POID", nullable = false)
    private Long incomeGlPoid;

    @Column(name = "DELETED", length = 1)
    private String deleted;

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
            createdBy = com.asg.common.lib.security.util.UserContext.getUserName();
        }
        if (deleted == null) {
            deleted = "N";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = LocalDateTime.now();
        lastModifiedBy = com.asg.common.lib.security.util.UserContext.getUserName();
    }
}