package com.asg.shipping.demurragedetentionpayabletransfer.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Entity class for SHIP_DEM_DETN_TRANSFER_HDR table
 */
@Entity
@Table(name = "SHIP_DEM_DETN_TRANSFER_HDR",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_SHIP_DEM_DETN_TRANSFER_HDR", columnNames = {"DOC_REF"})
        })
@Setter
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ShipDemDetnTransferHdr extends BaseEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionPoid;

    @Column(name = "GROUP_POID", nullable = false)
    @AuditIgnore
    private Long groupPoid;

    @Column(name = "COMPANY_POID", nullable = false)
    @AuditIgnore
    private Long companyPoid;

    @Column(name = "DOC_REF", length = 25, unique = true)
    private String docRef;

    @Column(name = "TRANSACTION_DATE")
    @AuditIgnore
    private LocalDate transactionDate;

    @Column(name = "LINE_POID")
    private Long linePoid;

    @Column(name = "BL_TYPE", length = 25)
    private String blType;

    @Column(name = "EMPTY_FROM_DATE")
    @AuditIgnore
    private LocalDate emptyFromDate;

    @Column(name = "EMPTY_TO_DATE")
    @AuditIgnore
    private LocalDate emptyToDate;

    @Column(name = "PAYABLE_GL_POID", nullable = false)
    private Long payableGlPoid;

    @Column(name = "INCOME_GL_POID", nullable = false)
    private Long incomeGlPoid;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @PrePersist
    protected void onCreate() {
        if (deleted == null) {
            deleted = "N";
        }
    }
}