package com.asg.shipping.common.entity;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "GLOBAL_COUNTRY_MASTER")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalCountryMaster {

    @Id
    @Column(name = "COUNTRY_POID")
    private Long countryPoid;

    @Column(name = "GROUP_POID")
    private Long groupoid;

    @Column(name = "COUNTRY_CODE")
    private String countryCode;

    @Column(name = "COUNTRY_NAME")
    private String countryName;

    @Column(name = "COUNTRY_NAME2")
    private String countryName2;

    @Column(name = "REGION_POID")
    private Long regionPoid;

    @Column(name = "ACTIVE")
    private String active;

    @Column(name = "SEQNO")
    private Long seqno;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "LASTMODIFIED_BY")
    private String lastmodifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private Timestamp lastModifieddate;

    @Column(name = "DELETED")
    private String deleted;

    @Column(name = "MANIFEST_COUNTRY_REMARK")
    private String manifestCountryRemark;

    @Column(name = "OFAC_BAN")
    private String ofacBan;

    @Column(name = "TRADE_BAN")
    private String tradeBan;

    @Column(name = "COUNTRY_TICKET_RATE")
    private Long countryTicketRate;
}
