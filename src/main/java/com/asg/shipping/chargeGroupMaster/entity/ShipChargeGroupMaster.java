package com.asg.shipping.chargeGroupMaster.entity;


import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "SHIP_CHARGE_GROUP_MASTER",
        uniqueConstraints = {
                @UniqueConstraint(name = "SHIP_CHARGE_GROUP_MAST_UK_CODE", columnNames = "CHARGE_GROUP_CODE"),
                @UniqueConstraint(name = "SHIP_CHARGE_GROUP_MAST_UK_NAME", columnNames = "CHARGE_GROUP_NAME")
        }
)
public class ShipChargeGroupMaster extends BaseEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CHARGE_GROUP_POID", nullable = false, updatable = false)
    private Long chargeGroupPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "CHARGE_GROUP_CODE", nullable = false, length = 20)
    private String chargeGroupCode;

    @Column(name = "CHARGE_GROUP_NAME", nullable = false, length = 1000)
    private String chargeGroupName;

    @Column(name = "CHARGE_GROUP_NAME2", length = 200)
    private String chargeGroupName2;

    @Column(name = "CHARGE_GL_PAYABLE")
    private Long chargeGlPayable;

    @Column(name = "LINEWISE_PAYABLE_POSTING", length = 1)
    private String linewisePayablePosting;

    @Column(name = "CHARGE_GL_SALE")
    private Long chargeGlSale;

    @Column(name = "CHARGE_GL_COST_SALE")
    private Long chargeGlCostSale;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "SEQNO")
    private Long seqNo;

    @Column(name = "GL_PREFIX")
    private String glPrefix;
}
