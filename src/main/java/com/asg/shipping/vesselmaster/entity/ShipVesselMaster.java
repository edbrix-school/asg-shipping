package com.asg.shipping.vesselmaster.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity class for SHIP_VESSEL_MASTER table
 */
@Entity
@Table(name = "SHIP_VESSEL_MASTER")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipVesselMaster extends BaseEntity {

    @Id
    @Column(name = "VESSEL_POID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // @SequenceGenerator(name = "vessel_seq", sequenceName = "SHIP_VESSEL_MASTER_SEQ", allocationSize = 1)
    private Long vesselPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "VESSEL_CODE", nullable = false, length = 20, updatable = false)
    private String vesselCode;

    @Column(name = "VESSEL_NAME", nullable = false, length = 100)
    private String vesselName;

    @Column(name = "VESSEL_NAME2", length = 100)
    private String vesselName2;

    @Column(name = "LINE_POID")
    private Long linePoid;

    @Column(name = "OWNER", length = 100)
    private String owner;

    @Column(name = "AGENT_POID")
    private Long agentPoid;

    @Column(name = "REGISTRATION_NO", length = 100)
    private String registrationNo;

    @Column(name = "REGISTRATION_DATE")
    private LocalDate registrationDate;

    @Column(name = "COUNTRY_OF_REGISTRATION", length = 50)
    private String countryOfRegistration;

    @Column(name = "FLAG_OF_COUNTRY", length = 100)
    private String flagOfCountry;

    @Column(name = "VESSEL_TYPE_POID")
    private Long vesselTypePoid;

    @Column(name = "VESSEL_TYPE_CLASS", length = 20)
    private String vesselTypeClass;

    @Column(name = "GRT", precision = 18, scale = 2)
    private BigDecimal grt;

    @Column(name = "NRT", precision = 18, scale = 2)
    private BigDecimal nrt;

    @Column(name = "DWT", precision = 18, scale = 2)
    private BigDecimal dwt;

    @Column(name = "VESSEL_LENGTH", precision = 18, scale = 2)
    private BigDecimal vesselLength;

    @Column(name = "BEAM", precision = 18, scale = 2)
    private BigDecimal beam;

    @Column(name = "DRAFT", precision = 18, scale = 2)
    private BigDecimal draft;

    @Column(name = "HATCHES")
    private BigDecimal hatches;

    @Column(name = "BAYHATCH")
    private BigDecimal bayhatch;

    @Column(name = "IMO_NUMBER", length = 20, unique = true)
    private String imoNumber;

    @Column(name = "REMARKS", length = 20)
    private String remarks;

    @Column(name = "LINE_NAME", length = 20)
    private String lineName;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "SEQNO", precision = 5)
    private Integer seqno;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @PrePersist
    protected void onCreate() {
        if (deleted == null) {
            deleted = "N";
        }
        if (active == null) {
            active = "Y";
        }
    }

 /*   @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = LocalDateTime.now();
    }*/
}

