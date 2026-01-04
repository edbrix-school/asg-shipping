package com.asg.shipping.containertypeportchargestariff.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "SHIP_PORT_CHARGES_DTL")
@IdClass(ShipPortChargesDtlId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShipPortChargesDtl {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "CHARGE_CODE_POID")
    private Long chargeCodePoid;

    @Column(name = "CHARGE_TYPE_APPLICABLE", length = 25)
    private String chargeTypeApplicable;

    @Column(name = "CHARGE_APPLICABLE", length = 25)
    private String chargeApplicable;

    @Column(name = "IMCO_CLASS_TYPE", length = 25)
    private String imcoClassType;

    @Column(name = "OOG_TYPE", length = 25)
    private String oogType;

    @Column(name = "OTHERS_TYPE", length = 25)
    private String othersType;

    @Column(name = "AMOUNT_20", precision = 25, scale = 3)
    private BigDecimal amount20;

    @Column(name = "AMOUNT_40", precision = 25, scale = 3)
    private BigDecimal amount40;

    @Column(name = "AMOUNT_OTHER", precision = 25, scale = 3)
    private BigDecimal amountOther;

    @Column(name = "AMOUNT_20_COST", precision = 25, scale = 3)
    private BigDecimal amount20Cost;

    @Column(name = "AMOUNT_40_COST", precision = 25, scale = 3)
    private BigDecimal amount40Cost;

    @Column(name = "AMOUNT_OTHER_COST", precision = 25, scale = 3)
    private BigDecimal amountOtherCost;

    @Column(name = "AMOUNT_53", precision = 25, scale = 3)
    private BigDecimal amount53;

    @Column(name = "AMOUNT_53_COST", precision = 25, scale = 3)
    private BigDecimal amount53Cost;

    @Column(name = "SHIP_CHARGE_TYPE", length = 100)
    private String shipChargeType;
}
