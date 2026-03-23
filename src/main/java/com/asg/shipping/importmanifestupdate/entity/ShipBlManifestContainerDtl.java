package com.asg.shipping.importmanifestupdate.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "SHIP_BL_MANIFEST_CONTAINER_DTL",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_SHIP_BLCNTDTL",
                        columnNames = {"TRANSACTION_POID", "CONTAINER_NO"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipBlManifestContainerDtl {

    @EmbeddedId
    private ShipBlManifestDtlId id;

    @Column(name = "MATE_TRANSACTION_POID")
    private Long mateTransactionPoid;

    @Column(name = "CONTAINER_NO", length = 25)
    private String containerNo;

    @Column(name = "EQUIPMENT_SHIPPER_OWN", length = 1)
    private String equipmentShipperOwn;

    @Column(name = "CARGO_DESCRIPTION", length = 200)
    private String cargoDescription;

    @Column(name = "EQUIPMENT_SEAL_NO", length = 25)
    private String equipmentSealNo;

    @Column(name = "EQUIPMENT_ISO_TYPE", length = 20)
    private String equipmentIsoType;

    @Column(name = "EQUIPMENT_TYPE", length = 20)
    private String equipmentType;

    @Column(name = "EQUIPMENT_SIZE", length = 20)
    private String equipmentSize;

    @Column(name = "QUANTITY")
    private Long quantity;

    @Column(name = "GRS_VOLUME")
    private Long grsVolume;

    @Column(name = "GRS_WEIGHT")
    private Long grsWeight;

    @Column(name = "NET_VOLUME")
    private Long netVolume;

    @Column(name = "NET_WEIGHT")
    private Long netWeight;

    @Column(name = "TARE_WEIGHT")
    private Long tareWeight;

    @Column(name = "NO_OF_PACKS")
    private Long noOfPacks;

    @Column(name = "PACK_UNIT", length = 6)
    private String packUnit;

    @Column(name = "COMODITY_POID")
    private Long comodityPoid;

    @Column(name = "DESTINATION_PORT_POID")
    private Long destinationPortPoid;

    @Column(name = "IMO", length = 200)
    private String imo;

    @Column(name = "OOG_L", length = 20)
    private String oogL;

    @Column(name = "OOG_B", length = 20)
    private String oogB;

    @Column(name = "OOG_H", length = 20)
    private String oogH;

    @Column(name = "REFFER_TEMP", length = 20)
    private String refferTemp;

    @Column(name = "REFFER_HUM", length = 20)
    private String refferHum;

    @Column(name = "REFFER_VENT", length = 20)
    private String refferVent;

    @Column(name = "ISSUE_TO_CONSIGNEE")
    private LocalDate issueToConsignee;

    @Column(name = "RETURN_FROM_CONSIGNEE")
    private LocalDate returnFromConsignee;

    @Column(name = "IS_IMCO", length = 1)
    private String isImco;

    @Column(name = "IS_OOG", length = 1)
    private String isOog;

    @Column(name = "IS_REFER", length = 1)
    private String isRefer;

    @Column(name = "REFER_TYPE", length = 50)
    private String referType;

    @Column(name = "IMCO_CLASS_TYPE", length = 25)
    private String imcoClassType;

    @Column(name = "GUARANTEE_FLAG", length = 1)
    private String guaranteeFlag;

    @Column(name = "GUARANTEED_BY", length = 25)
    private String guaranteedBy;

    @Column(name = "EXTRA_FREE_DAYS")
    private Long extraFreeDays;

    @Column(name = "EXTRA_FREE_DAYS_PRNPLS")
    private Long extraFreeDaysPrnpls;

    @Column(name = "OOG_L_W", length = 20)
    private String oogLW;

    @Column(name = "OOG_R_W", length = 20)
    private String oogRW;

    @Column(name = "OOG_F", length = 20)
    private String oogF;

    @Column(name = "OOG_A", length = 20)
    private String oogA;

    @Column(name = "OOG_TYPE", length = 25)
    private String oogType;

    @Column(name = "IMCO_CLASS_ACTUAL", length = 100)
    private String imcoClassActual;

    @Column(name = "DISPLAY_COLLECTED_DATE")
    private LocalDate displayCollectedDate;

    @Column(name = "TOTAL_DAYS_COLLECTED")
    private Long totalDaysCollected;

    @Column(name = "TOTAL_AMOUNT_COLLECTED")
    private Long totalAmountCollected;

    @Column(name = "PRINT_RETURN_FORM_DEFAULT", length = 25)
    private String printReturnFormDefault;

    @Column(name = "PRINT_DELIVERY_FORM_DEFAULT", length = 25)
    private String printDeliveryFormDefault;

    @Column(name = "DEM_DTT_PBLE_TRNSFD", length = 10)
    private String demDttPbleTrnsfd;

    @Column(name = "HS_CODE", length = 100)
    private String hsCode;

    @Column(name = "HS_DESCRIPTION", length = 200)
    private String hsDescription;

    @Column(name = "AMOUNT_PER_DAY_AFTER_FREE")
    private Long amountPerDayAfterFree;

    @Column(name = "ACTUAL_DISCHARGE_DATE")
    private LocalDate actualDischargeDate;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;
}
