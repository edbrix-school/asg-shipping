package com.asg.shipping.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "SHIP_CHARGE_MASTER",
        uniqueConstraints = {
                @UniqueConstraint(name = "SHIP_CHARGE_MASTER_UK_CODE",
                        columnNames = {"CHARGE_CODE", "DIVISION_CODE"}),
                @UniqueConstraint(name = "SHIP_CHARGE_MASTER_UK_NAME",
                        columnNames = {"CHARGE_NAME", "DIVISION_CODE"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShipChargeMaster {

    @Id
    @Column(name = "CHARGE_POID", nullable = false)
    private BigDecimal chargePoid;  // Trigger will populate this, so no @GeneratedValue

    @Column(name = "GROUP_POID")
    private BigDecimal groupPoid;

    @Column(name = "CHARGE_CODE", nullable = false, length = 20)
    private String chargeCode;

    @Column(name = "CHARGE_NAME", nullable = false, length = 1000)
    private String chargeName;

    @Column(name = "CHARGE_NAME2", length = 200)
    private String chargeName2;

    @Column(name = "CHARGE_REVENUE_TYPE", length = 50)
    private String chargeRevenueType;

    @Column(name = "CHARGE_TYPE", length = 50)
    private String chargeType;

    @Column(name = "CHARGE_GL_REVENUE")
    private BigDecimal chargeGlRevenue;

    @Column(name = "CHARGE_GL_COST")
    private BigDecimal chargeGlCost;

    @Column(name = "CHARGE_GL_WIP")
    private BigDecimal chargeGlWip;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "SEQNO")
    private BigDecimal seqNo;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "CHARGE_APPLICABLE_TYPE", length = 25)
    private String chargeApplicableType;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "CHARGE_PAYABLE_GL")
    private BigDecimal chargePayableGl;

    @Column(name = "DIVISION_CODE", nullable = false, length = 25)
    private String divisionCode;

    @Column(name = "FDA_GL_REVENUE")
    private BigDecimal fdaGlRevenue;

    @Column(name = "FDA_GL_COST")
    private BigDecimal fdaGlCost;

    @Column(name = "SH_FF_CHARGE_MAP")
    private BigDecimal shFfChargeMap;

    @Column(name = "SH_FF_CHARGE_GL_POID")
    private BigDecimal shFfChargeGlPoid;

    @Column(name = "SH_FF_CHARGE_GL_POID_REV")
    private BigDecimal shFfChargeGlPoidRev;

    @Column(name = "VISIBLE_IN_FF", length = 1)
    private String visibleInFf;

    @Column(name = "TAX_POID", nullable = false)
    private BigDecimal taxPoid;

    @Column(name = "INPUT_TAX_POID")
    private BigDecimal inputTaxPoid;

    @Column(name = "OLD_CHARGE_GL_REVENUE")
    private BigDecimal oldChargeGlRevenue;

    @Column(name = "OLD_CHARGE_GL_COST")
    private BigDecimal oldChargeGlCost;

    @Column(name = "DIRECT_REVENUE_GL")
    private BigDecimal directRevenueGl;

    @Column(name = "CHARGE_GROUP_POID")
    private BigDecimal chargeGroupPoid;

    @Column(name = "DIRECT_COST_OF_SALE_GL")
    private BigDecimal directCostOfSaleGl;

    @Column(name = "DIRECT_PAYABLE_GL")
    private BigDecimal directPayableGl;
}


