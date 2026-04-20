package com.asg.shipping.vesselvoyagecreation.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "SHIP_VOYAGE_TRANSHIP_DTL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ShipVoyageTranshipDtlId.class)
public class ShipVoyageTranshipDtlEntity extends BaseEntity {

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

    @Column(name = "SEAL_NO", length = 25)
    private String sealNo;

    @Column(name = "SEAL_NO2", length = 25)
    private String sealNo2;

    @Column(name = "SEAL_NO3", length = 25)
    private String sealNo3;

    @Column(name = "SEAL_KIND_CODE", length = 25)
    private String sealKindCode;

    @Column(name = "SEAL_KIND_CODE1", length = 25)
    private String sealKindCode1;

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

    @Column(name = "REFFER_HUM", length = 20)
    private String refferHum;

    @Column(name = "REFFER_VENT", length = 20)
    private String refferVent;

    @Column(name = "IMCO_CLASS_ACTUAL", length = 100)
    private String imcoClassActual;

    @Column(name = "IMO", length = 20)
    private String imo;

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

    @Column(name = "OOG_B", length = 20)
    private String oogB;

    @Column(name = "OOG_F", length = 20)
    private String oogF;

    @Column(name = "OOG_A", length = 20)
    private String oogA;

    @Column(name = "OOG_TYPE", length = 25)
    private String oogType;

    @Column(name = "LOAD_WEIGHT_KG", length = 25)
    private String loadWeightKg;

    @Column(name = "WEIGHT_KG", length = 25)
    private String weightKg;

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

}










