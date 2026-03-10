package com.asg.shipping.shippingFFChargeMaster.entity;


import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "SHIP_CHARGE_MASTER")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipChargeMaster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CHARGE_POID", nullable = false, updatable = false)
    private Long chargePoid;

    @Column(name = "GROUP_POID", nullable = false)
    private Long groupPoid;

    @Column(name = "CHARGE_CODE", length = 20, updatable = false)
    private String chargeCode;

    @Column(name = "CHARGE_NAME", length = 100)
    private String chargeName;

    @Column(name = "CHARGE_NAME2", length = 100)
    private String chargeName2;

    @Column(name = "CHARGE_REVENUE_TYPE", length = 50)
    private String chargeRevenueType;

    @Column(name = "CHARGE_TYPE", length = 50)
    private String chargeType;

    @Column(name = "CHARGE_APPLICABLE_TYPE", length = 25)
    private String chargeApplicableType;

    @Column(name = "DIVISION_CODE", length = 25)
    private String divisionCode;

    @Column(name = "CHARGE_GL_REVENUE")
    private Long chargeGlRevenue;

    @Column(name = "CHARGE_GL_COST")
    private Long chargeGlCost;

    @Column(name = "CHARGE_GL_WIP")
    private Long chargeGlWip;

    @Column(name = "CHARGE_PAYABLE_GL")
    private Long chargePayableGl;

    @Column(name = "FDA_GL_REVENUE")
    private Long fdaGlRevenue;

    @Column(name = "FDA_GL_COST")
    private Long fdaGlCost;

    @Column(name = "DIRECT_REVENUE_GL")
    private Long directRevenueGl;

    @Column(name = "DIRECT_COST_OF_SALE_GL")
    private Long directCostOfSaleGl;

    @Column(name = "DIRECT_PAYABLE_GL")
    private Long directPayableGl;

    @Column(name = "TAX_POID")
    private Long taxPoid;

    @Column(name = "INPUT_TAX_POID")
    private Long inputTaxPoid;

    @Column(name = "CHARGE_GROUP_POID")
    private Long chargeGroupPoid;

    @Column(name = "SH_FF_CHARGE_MAP")
    private Long shFfChargeMap;

    @Column(name = "SH_FF_CHARGE_GL_POID")
    private Long shFfChargeGlPoid;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "SEQNO")
    private Integer seqno;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name= "SH_FF_CHARGE_GL_POID_REV", length = 22)
    private Long shFfChargeGlPoidRev;

    @Column(name = "VISIBLE_IN_FF", length = 1)
    private String visibleInFf;

    @Column(name = "OLD_CHARGE_GL_REVENUE", length = 22)
    private Long oldChargeGlRevenue;

    @Column(name = "OLD_CHARGE_GL_COST", length = 22)
    private Long oldChargeGlCost;

    @PrePersist
    protected void onCreate() {
        if (deleted == null) {
            deleted = "N";
        }
        if (active == null) {
            active = "Y";
        }
    }

  /*  @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = LocalDateTime.now();
    }*/
}
