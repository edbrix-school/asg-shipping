package com.asg.shipping.linepayabletransfetasperreporting.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.asg.common.lib.security.util.UserContext.getUserName;

/**
 * Entity class for SHIP_LINE_REPORT_TRANSFER_HDR table
 */
@Entity
@Table(name = "SHIP_LINE_REPORT_TRANSFER_HDR",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_SHIP_LINE_REPORT_TRANSFER_HDR_DOC_REF", columnNames = {"DOC_REF"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class ShipLineReportTransferHdr {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionPoid;

    @Column(name = "GROUP_POID", nullable = false)
    private Long groupPoid;

    @Column(name = "COMPANY_POID", nullable = false)
    private Long companyPoid;

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

    @Column(name = "LINE_POID")
    private Long linePoid;

    @Column(name = "BL_TYPE", length = 25)
    private String blType;

    @Column(name = "REPORT_START_DATE")
    private LocalDate reportStartDate;

    @Column(name = "REPORT_END_DATE")
    private LocalDate reportEndDate;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "DOC_REF", length = 25, unique = true)
    private String docRef;

    @Column(name = "DELETED", length = 1)
    @Builder.Default
    private String deleted = "N";

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
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
        if (lastModifiedBy == null) {
            lastModifiedBy = getUserName();
        }
    }
}