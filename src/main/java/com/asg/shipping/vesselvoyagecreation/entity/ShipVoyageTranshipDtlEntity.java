package com.asg.shipping.vesselvoyagecreation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "SHIP_VOYAGE_TRANSHIP_DTL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ShipVoyageTranshipDtlId.class)
public class ShipVoyageTranshipDtlEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "CONTAINER_NO", nullable = false, length = 25)
    private String containerNo;

    @Column(name = "CONTAINER_TYPE", length = 25)
    private String containerType;

    @Column(name = "ISO_CODE", length = 50)
    private String isoCode;

    @Column(name = "STATUS", length = 50)
    private String status;

    @Column(name = "ORIGIN", length = 100)
    private String origin;

    @Column(name = "POL", length = 100)
    private String pol;

    @Column(name = "LOAD_TRANSACTION_POID")
    private Long loadTransactionPoid;

    @Column(name = "IS_LOADED", length = 1)
    private String isLoaded;

    @Column(name = "IS_REFER", length = 1)
    private String isRefer;

    @Column(name = "REFFER_TEMP", length = 20)
    private String refferTemp;

    @Column(name = "IMO_CODE1", length = 25)
    private String imoCode1;

    @Column(name = "UN_NO1", length = 25)
    private String unNo1;

    @Column(name = "IMO_CODE2", length = 25)
    private String imoCode2;

    @Column(name = "UN_NO2", length = 25)
    private String unNo2;

    @Column(name = "OOG_H", length = 20)
    private String oogH;

    @Column(name = "OOG_L", length = 20)
    private String oogL;

    @Column(name = "OOG_L_W", length = 20)
    private String oogLW;

    @Column(name = "OOG_R_W", length = 20)
    private String oogRW;

    @Column(name = "LOAD_WEIGHT_KG", length = 25)
    private String loadWeightKg;

    @Column(name = "WEIGHT_TON", length = 25)
    private String weightTon;

    @Column(name = "HS_CODE", length = 100)
    private String hsCode;

    @Column(name = "HS_SHORTNAME", length = 100)
    private String hsShortname;

    @Column(name = "BLADING", length = 25)
    private String blading;

    @Column(name = "SLOT", length = 50)
    private String slot;

    @Column(name = "OUTBOUND_VESSEL", length = 150)
    private String outboundVessel;

    @Column(name = "LOAD_ORIGIN", length = 100)
    private String loadOrigin;

    @Column(name = "LOAD_FINAL_DESTINATION", length = 100)
    private String loadFinalDestination;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;
}










