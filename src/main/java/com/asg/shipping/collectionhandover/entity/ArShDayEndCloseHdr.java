package com.asg.shipping.collectionhandover.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity class for AR_SH_DAY_END_CLOSE_HDR table
 */
@Entity(name = "CollectionHandoverHdr")
@Table(name = "AR_SH_DAY_END_CLOSE_HDR")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArShDayEndCloseHdr extends BaseEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionPoid;

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "DOC_REF", length = 30)
    private String docRef;

    @Column(name = "LOCATION_CODE", length = 20)
    private String locationCode;

    @Column(name = "CASH_AMOUNT", precision = 18, scale = 2)
    private BigDecimal cashAmount;

    @Column(name = "CHEQUE_AMOUNT", precision = 18, scale = 2)
    private BigDecimal chequeAmount;

    @Column(name = "OUTSTANDING_AMOUNT", precision = 18, scale = 2)
    private BigDecimal outstandingAmount;

    @Column(name = "TOTAL_AMOUNT", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "NOOF_CHQS")
    private Integer noofChqs;

    @Column(name = "LOC_REMARKS", length = 500)
    private String locRemarks;

    @Column(name = "VERIFIED_RCVD", length = 1)
    private String verifiedRcvd;

    @Column(name = "MAIN_OFC_REMARKS", length = 500)
    private String mainOfcRemarks;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @PrePersist
    protected void onCreate() {
        // Don't auto-set transactionDate - let it be set explicitly
        if (deleted == null) {
            deleted = "N";
        }
        if (verifiedRcvd == null) {
            verifiedRcvd = "N";
        }
    }
}

