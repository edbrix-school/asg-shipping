package com.asg.shipping.demurragedetentionpayabletransfer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "SHIP_DEM_DTN_TRANSFER_BILL_DTL")
@IdClass(ShipDemDtnTransferBillDtlId.class)
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ShipDemDtnTransferBillDtl {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "GL_POID", nullable = false)
    private Long glPoid;

    @Column(name = "GL_COMPANY_POID")
    private Long glCompanyPoid;

    @Column(name = "BILL_REF_TYPE", length = 100)
    private String billRefType;

    @Column(name = "BILL_REFNO", length = 100)
    private String billRefno;

    @Column(name = "BILL_DUE_DATE")
    private LocalDate billDueDate;

    @Column(name = "DESCRIPTION", length = 200)
    private String description;

    @Column(name = "DR_AMT", precision = 20, scale = 3)
    private BigDecimal drAmt;

    @Column(name = "CR_AMT", precision = 20, scale = 3)
    private BigDecimal crAmt;

    @Column(name = "CONTAINER_NO", length = 50)
    private String containerNo;

    @Column(name = "BILLWISE_BALANCE", precision = 20, scale = 3)
    private BigDecimal billwiseBalance;

    @Column(name = "CHECKALL", length = 1)
    private String checkall;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID", insertable = false, updatable = false)
    private ShipDemDetnTransferHdr shipDemDetnTransferHdr;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        if (createdBy == null) {
            createdBy = com.asg.common.lib.security.util.UserContext.getUserName();
        }
        if (checkall == null) {
            checkall = "N";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = LocalDateTime.now();
        lastModifiedBy = com.asg.common.lib.security.util.UserContext.getUserName();
    }
}