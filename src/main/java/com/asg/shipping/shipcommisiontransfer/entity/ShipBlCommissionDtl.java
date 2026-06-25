package com.asg.shipping.shipcommisiontransfer.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entity class for SHIP_BL_COMMISSION_DTL table
 */
@Entity
@Table(name = "SHIP_BL_COMMISSION_DTL")
@IdClass(ShipBlCommissionDtlId.class)
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ShipBlCommissionDtl extends BaseEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "DESCRIPTION", length = 200)
    private String description;

    @Column(name = "BL_TRANSACTION_POID", nullable = false)
    private Long blTransactionPoid;

    @Column(name = "CURRENCY_CODE", length = 50)
    private String currencyCode;

    @Column(name = "CURRENCY_EXCHANGE", precision = 20, scale = 3)
    private BigDecimal currencyExchange;

    @Column(name = "QUANTITY_20", precision = 20, scale = 3)
    private BigDecimal quantity20;

    @Column(name = "QUANTITY_40", precision = 20, scale = 3)
    private BigDecimal quantity40;

    @Column(name = "BUY_PERCHARGE", precision = 20, scale = 3)
    private BigDecimal buyPercharge;

    @Column(name = "SELL_AMOUNT", precision = 20, scale = 3)
    private BigDecimal sellAmount;

    @Column(name = "COMMISSION_AMT", precision = 20, scale = 3)
    private BigDecimal commissionAmt;

    @Column(name = "FREIGHT_TYPE", length = 25)
    private String freightType;

    @Column(name = "SELECTED", length = 1)
    private String selected;

    @Column(name = "COMMITION_ON_AMOUNT", precision = 20, scale = 3)
    private BigDecimal commitionOnAmount;

    @Column(name = "BUY_PERCHARGE_FRT", precision = 20, scale = 3)
    private BigDecimal buyPerchargeFrt;

    @Column(name = "SELL_AMOUNT_FRT", precision = 20, scale = 3)
    private BigDecimal sellAmountFrt;

    @Column(name = "DRILLDOWN_LINK_INFO", length = 200)
    private String drilldownLinkInfo;

    @Column(name = "BL_TYPE", length = 25)
    private String blType;

    @Column(name = "BL_STATUS", length = 25)
    private String blStatus;

    @Column(name = "THC_AMOUNT", precision = 20, scale = 3)
    private BigDecimal thcAmount;

    @Column(name = "COMMISSION_HAND_AMT", precision = 20, scale = 3)
    private BigDecimal commissionHandAmt;

    @Column(name = "COMMISSION_ADJ_AMT", precision = 25, scale = 3)
    private BigDecimal commissionAdjAmt;

    @Column(name = "SHORT_LEG_SELECTED", length = 25)
    private String shortLegSelected;

    @AuditIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID", insertable = false, updatable = false)
    private ShipBlCommissionHdr shipBlCommissionHdr;

}