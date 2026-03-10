package com.asg.shipping.shippingmanifestcorrector.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.asg.common.lib.security.util.UserContext.*;

/**
 * Entity class for SHIP_BL_REPRINT_CHARGE_DTL table
 */
@Entity
@Table(name = "SHIP_BL_REPRINT_CHARGE_DTL")
@IdClass(ShipBlReprintChargeDtlId.class)
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class ShipBlReprintChargeDtl extends BaseEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "CHARGE_POID")
    private Long chargePoid;

    @Column(name = "CURRENCY_EXCHANGE")
    private BigDecimal currencyExchange;

    @Column(name = "QUANTITY", nullable = false)
    private BigDecimal quantity;

    @Column(name = "BUY_PERCHARGE")
    private BigDecimal buyPercharge;

    @Column(name = "PER_QUANTITY_AMOUNT")
    private BigDecimal perQuantityAmount;

    @Column(name = "PAID_AT_PORT_POID")
    private Long paidAtPortPoid;


    @Column(name = "CHARGE_TYPE", length = 25)
    @Builder.Default
    private String chargeType = "MANIFEST";

    @Column(name = "CURRENCY_CODE", length = 50)
    private String currencyCode;

    @Column(name = "FREIGHT_TYPE", length = 25, nullable = false)
    private String freightType;

    @Column(name = "EDI_CHARGE_CODE", length = 50)
    private String ediChargeCode;

    @Column(name = "AR_SH_RECEIPT_TRANSACTION_POID")
    private Long arShReceiptTransactionPoid;

    @Column(name = "CHARGE_BASIS_ON", length = 20)
    private String chargeBasisOn;

    @Column(name = "PRINT_GROUP", length = 50)
    private String printGroup;

    @Column(name = "RECEIPT_INVOICE_POID")
    private Long receiptInvoicePoid;

    @Column(name = "DOC_REF_LINK_NO", length = 100)
    private String docRefLinkNo;

    @Column(name = "CONTAINER_NUMBER", length = 25)
    private String containerNumber;

    @Column(name = "REV_PAYABLE")
    private BigDecimal revPayable;

    @Column(name = "REV_INCOME")
    private BigDecimal revIncome;

}

