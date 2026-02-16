package com.asg.shipping.lineprofile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "SH_LINE_PROFILE_CONTACT_DTL")
@IdClass(ShipLineProfileContactDtlId.class)
@Getter
@Setter
public class ShipLineProfileContactDtlEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long lineProfilePoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "CONTACT_NAME")
    private String contactName;

    @Column(name = "DESIGNATION")
    private String designation;

    @Column(name = "MOBILE")
    private String mobile;

    @Column(name = "LANDLINE")
    private String landline;

    @Column(name = "EMAIL_ADDRESS")
    private String emailAddress;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;
}

