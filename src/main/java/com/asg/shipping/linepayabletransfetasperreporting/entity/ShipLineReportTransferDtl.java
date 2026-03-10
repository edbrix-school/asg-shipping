package com.asg.shipping.linepayabletransfetasperreporting.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.asg.common.lib.security.util.UserContext.getUserName;
/**
 * Entity class for SHIP_LINE_REPORT_TRANSFER_DTL table
 */
@Entity
@Table(name = "SHIP_LINE_REPORT_TRANSFER_DTL")
@IdClass(ShipLineReportTransferDtlId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class ShipLineReportTransferDtl extends BaseEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "MAINFEST_TRANSACTION_POID")
    private Long mainfestTransactionPoid;

    @Column(name = "BL_NUMBER", length = 50)
    private String blNumber;

    @Column(name = "ACUTAL_AMOUNT", precision = 20, scale = 3)
    private BigDecimal acutalAmount;

    @Column(name = "TOTAL_AMOUNT_TRANSFER", precision = 20, scale = 3)
    private BigDecimal totalAmountTransfer;

    @Column(name = "IS_SELECT", length = 25)
    @Builder.Default
    private String isSelect = "N";

    @Column(name = "CHARGE_POID")
    private Long chargePoid;

    @Column(name = "FREIGHT_TYPE", length = 25)
    private String freightType;

    @Column(name = "CURRENCY_CODE", length = 25)
    private String currencyCode;

    @Column(name = "CURRENCY_EXCHANGE")
    private BigDecimal currencyExchange;

    @Column(name = "CURRENCY_AMOUNT")
    private BigDecimal currencyAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID", insertable = false, updatable = false)
    private ShipLineReportTransferHdr header;

}
