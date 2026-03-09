package com.asg.shipping.agentMaster.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "SHIP_AGENT_MASTER")
public class ShipAgentMasterEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AGENT_POID", nullable = false, updatable = false)
    private Long agentPoid;

    @Column(name = "GROUP_POID", nullable = false)
    private Long groupPoid;

    @Column(name = "AGENT_NAME", nullable = false, length = 100)
    private String agentName;

    @Column(name = "AGENT_NAME2", length = 100)
    private String agentName2;

    @Column(name = "DETAILS", nullable = false, length = 200)
    private String details;

    @Column(name = "LINE_POID")
    private Long linePoid;

    @Column(name = "PORT_POID")
    private Long portPoid;

    @Column(name = "CONTACT_PERSON", length = 100)
    private String contactPerson;

    @Column(name = "EMAIL", length = 100)
    private String email;

    @Column(name = "CONTACT_NO", length = 100)
    private String contactNo;

    @Column(name = "FAX_NO", length = 100)
    private String faxNo;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "SEQNO")
    private Integer seqNo;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "COUNTRY_POID")
    private Long countryPoid;
}
