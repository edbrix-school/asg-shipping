package com.asg.shipping.remuneration.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "SHIP_REMUNERATION_MASTER")
public class ShipRemunerationMaster extends BaseEntity {

    @AuditIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REMUNERATION_POID", nullable = false)
    private Long remunerationPoid;

    @Column(name = "REMUN_CODE", length = 20)
    private String remunCode;

    @Column(name = "REMUN_DESCRIPTION", length = 100)
    private String remunDescription;

    @Column(name = "IMP_EXP_TYPE", length = 20)
    private String impExpType;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "SEQNO", precision = 5)
    private Integer seqNo;

    @Column(name = "REMUN_BASED_ON", length = 20)
    private String remunBasedOn;

    @Column(name = "REMUN_CHARGE_CODE_POID", length = 20)
    private String remunChargeCodePoid;

    @Column(name = "GL_POID")
    private Long glPoid;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "REMUN_BOOKED_BY_USED", length = 1)
    private String remunBookedByUsed;
}

