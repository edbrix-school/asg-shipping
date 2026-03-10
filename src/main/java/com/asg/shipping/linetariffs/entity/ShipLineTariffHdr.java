package com.asg.shipping.linetariffs.entity;

import com.asg.common.lib.entity.BaseEntity;
import com.asg.common.lib.utility.DateUtil;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Entity class for SHIP_LINE_TARIFF_HDR table
 */
@Entity
@Table(name = "SHIP_LINE_TARIFF_HDR",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_DOCREFFSHIP_LINE_TARIFF_HDR", columnNames = {"DOC_REF"})
        })
@Data
@NoArgsConstructor
public class ShipLineTariffHdr extends BaseEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "line_tariff_seq")
    @SequenceGenerator(name = "line_tariff_seq", sequenceName = "SHIP_LINE_TARIFF_HDR_SEQ", allocationSize = 1)
    private Long transactionPoid;

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "LINE_POID")
    private Long linePoid;

    @Column(name = "DESCRIPTION", length = 100)
    private String description;

    @Column(name = "PERIOD_FROM")
    private LocalDate periodFrom;

    @Column(name = "PERIOD_TO")
    private LocalDate periodTo;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "DMG_FROM_SAMEDAY", length = 1)
    private String dmgFromSameday;

    @Column(name = "DMG_FROM_NEXTDAY", length = 1)
    private String dmgFromNextday;

    @Column(name = "DMG_SKIP_HOLIDAYS", length = 1)
    private String dmgSkipHolidays;

    @Column(name = "DMG_SKIP_WEEKENDS", length = 1)
    private String dmgSkipWeekends;

    @Column(name = "DMG_BASESLAB_AFTER_FREE", length = 1)
    private String dmgBaseslabAfterFree;

    @Column(name = "DTN_FROM_SAMEDAY", length = 1)
    private String dtnFromSameday;

    @Column(name = "DTN_FROM_NEXTDAY", length = 1)
    private String dtnFromNextday;

    @Column(name = "DTN_SKIP_HOLIDAYS", length = 1)
    private String dtnSkipHolidays;

    @Column(name = "DTN_SKIP_WEEKENDS", length = 1)
    private String dtnSkipWeekends;

    @Column(name = "DTN_BASESLAB_AFTER_FREE", length = 1)
    private String dtnBaseslabAfterFree;

    @Column(name = "PAYABLE_CURRENCY", length = 25)
    private String payableCurrency;

    @Column(name = "RECEIVABLE_CURRENCY", length = 25)
    private String receivableCurrency;

    @Column(name = "DOC_REF", length = 25, unique = true)
    private String docRef;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "SEQNO")
    private Integer seqno;

    @PrePersist
    protected void onCreate() {

        if (transactionDate == null) {
            transactionDate = DateUtil.getCurrentDateInUserTimeZone();
        }
        if (deleted == null) {
            deleted = "N";
        }
    }

}

