package com.asg.shipping.lineprofile.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "SH_LINE_PROFILE_MASTER")
@Getter
@Setter
public class ShipLineProfileMasterEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LINE_PROFILE_POID", nullable = false)
    private Long lineProfilePoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "LINE_POID")
    private Long linePoid;

    @Column(name = "REGION")
    private String region;

    @Column(name = "REMARKS")
    private String remarks;

    @Column(name = "AGREEMENT_POID")
    private Long agreementPoid;

    @Lob
    @Column(name = "LOGO_IMAGE")
    private byte[] logoImage;

    @Column(name = "ACTIVE")
    private String active;

    @Column(name = "SEQNO")
    private Long seqNo;


    @Column(name = "DELETED")
    private String deleted;
}

