package com.asg.shipping.regionmaster.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "SHIP_REGION_MASTER")
public class ShipRegionMasterEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REGION_POID", nullable = false)
    private Long regionPoid;

    @Column(name = "GROUP_POID", nullable = false)
    private Long groupPoid;

    @Column(name = "REGION_CODE", nullable = false, length = 20)
    private String regionCode;

    @Column(name = "REGION_NAME", nullable = false, length = 100)
    private String regionName;

    @Column(name = "REGION_NAME2", length = 100)
    private String regionName2;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "SEQNO")
    private Long seqno;

    @Column(name = "DELETED", length = 1)
    private String deleted;
}

