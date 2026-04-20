package com.asg.shipping.tradelanemaster.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "SHIP_TRADELANE_MASTER")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipTradelaneMaster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRADELANE_POID", nullable = false)
    private Long tradeLanePoid;

    @Column(name = "GROUP_POID", nullable = false)
    private Long groupPoid;

    @Column(name = "TRADELANE_CODE", length = 20, nullable = false, unique = true)
    private String tradeLaneCode;

    @Column(name = "TRADELANE_NAME", length = 100, nullable = false, unique = true)
    private String tradeLaneName;

    @Column(name = "TRADELANE_NAME2", length = 100)
    private String tradeLaneName2;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "SEQNO")
    private Long seqNo;


    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "REGION_POID")
    private Long regionPoid;
}
