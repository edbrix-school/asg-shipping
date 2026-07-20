package com.asg.shipping.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "GLOBAL_ADDRESS_DETAILS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalAddressDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ADDRESS_POID", nullable = false)
    private BigDecimal addressPoid;

    @Column(name = "ADDRESS_MASTER_POID", nullable = false)
    private Long addressMasterPoid;

    @Column(name = "ADDRESS_TYPE", length = 50)
    private String addressType;

    @Column(name = "OFF_TEL1", length = 50)
    private String offTel1;

    @Column(name = "OFF_TEL2", length = 50)
    private String offTel2;

    @Column(name = "CONTACT_PERSON", length = 200)
    private String contactPerson;

    @Column(name = "DESIGNATION", length = 100)
    private String designation;

    @Column(name = "MOBILE", length = 50)
    private String mobile;

    @Column(name = "FAX", length = 50)
    private String fax;

    @Column(name = "EMAIL1", length = 200)
    private String email1;

    @Column(name = "EMAIL2", length = 200)
    private String email2;

    @Column(name = "WEBSITE", length = 200)
    private String website;

    @Column(name = "PO_BOX", length = 50)
    private String poBox;

    @Column(name = "OFF_NO", length = 50)
    private String offNo;

    @Column(name = "BLDG", length = 100)
    private String bldg;

    @Column(name = "ROAD", length = 200)
    private String road;

    @Column(name = "AREA_CITY", length = 200)
    private String areaCity;

    @Column(name = "STATE", length = 100)
    private String state;

    @Column(name = "COUNTRY_POID")
    private Long countryPoid;

    @Column(name = "LAND_MARK", length = 200)
    private String landMark;

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

    @Column(name = "OLD_ACCNO_REF", length = 50)
    private String oldAccnoRef;

    @Column(name = "OLD_GL_ACCTNO", length = 50)
    private String oldGlAcctno;

    @Column(name = "OLD_GL_ACCTNO_SUPPLIER", length = 50)
    private String oldGlAcctnoSupplier;

    @Column(name = "VERIFIED", length = 1)
    private String verified;

    @Column(name = "VERIFIED_BY", length = 50)
    private String verifiedBy;

    @Column(name = "VERIFIED_DATE")
    private Timestamp verifiedDate;

    @Column(name = "CITY", length = 100)
    private String city;

    @Column(name = "WHATSAPP_NO", length = 50)
    private String whatsappNo;

    @Column(name = "LINKEDIN", length = 200)
    private String linkedin;

    @Column(name = "INSTAGRAM", length = 200)
    private String instagram;

    @Column(name = "FACEBOOK", length = 200)
    private String facebook;
}
