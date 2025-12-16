package com.asg.shipping.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Entity
@Table(name = "GLOBAL_ADDRESS_MASTER")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalAddressMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ADDRESS_MASTER_POID", nullable = false)
    private Long addressMasterPoid;

    @Column(name = "ADDRESS_NAME", length = 200)
    private String addressName;

    @Column(name = "ADDRESS_NAME2", length = 200)
    private String addressName2;

    @Column(name = "COUNTRY_POID")
    private Long countryPoid;

    @Column(name = "STATE_POID")
    private Long statePoid;

    @Column(name = "GROUP_POID", nullable = false)
    private Long groupPoid;

    @Column(name = "CR_NUMBER", length = 50)
    private String crNumber;

    @Column(name = "CREATED_BY", length = 50)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 50)
    private String lastmodifiedBy;

    @UpdateTimestamp
    @Column(name = "LASTMODIFIED_DATE")
    private Timestamp lastmodifiedDate;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "IS_FORWARDER", length = 1)
    private String isForwarder;

    @Column(name = "PARTY_TYPE", length = 50)
    private String partyType;

    @Column(name = "PREFERRED_COMMUNICATION", length = 50)
    private String preferredCommunication;

    @Column(name = "REMARKS", length = 1000)
    private String remarks;

    @Column(name = "SEQNO")
    private Integer seqno;

    @Column(name = "OLD_ACCTNO", length = 50)
    private String oldAcctno;

    @Column(name = "WHATSAPP_NO", length = 50)
    private String whatsappNo;

    @Column(name = "FACEBOOK", length = 200)
    private String facebook;

    @Column(name = "INSTAGRAM", length = 200)
    private String instagram;

    @Column(name = "LINKEDIN", length = 200)
    private String linkedin;
}
