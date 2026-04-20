package com.asg.shipping.commoditymaster.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;


@Entity
@Table(name = "SHIP_COMODITY_MASTER")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommodityMaster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "COMODITY_POID", nullable = false, updatable = false)
    private Long commodityPoid;

    @Column(name = "GROUP_POID", nullable = false)
    private Long groupPoid;

    @Column(name = "COMODITY_CODE", length = 20)
    private String commodityCode;

    @Column(name = "COMODITY_NAME", length = 100, nullable = false)
    private String commodityName;

    @Column(name = "COMODITY_NAME2", length = 100)
    private String commodityName2;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "SEQNO")
    private Long seqno;

    @Column(name = "DELETED", length = 1)
    private String deleted;
}