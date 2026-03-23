package com.asg.shipping.importmanifestupdate.dto;

import com.asg.shipping.importManifestUpdate.service.BlManifestValidationService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerRequestDto implements BlManifestValidationService.ContainerValidatable {

    private Long detRowId;
    private String actionType; 
    private Long mateTransactionPoid;
    private String containerNo;
    private String equipmentShipperOwn;
    private String cargoDescription;
    private String equipmentSealNo;
    private String equipmentIsoType;
    private String equipmentType;
    private String equipmentSize;
    private Long quantity;
    private Long grsVolume;
    private Long grsWeight;
    private Long netVolume;
    private Long netWeight;
    private Long tareWeight;
    private Long noOfPacks;
    private String packUnit;
    private Long comodityPoid;
    private Long destinationPortPoid;
    private String imo;
    private String oogL;
    private String oogB;
    private String oogH;
    private String refferTemp;
    private String refferHum;
    private String refferVent;
    private LocalDate issueToConsignee;
    private LocalDate returnFromConsignee;
    private String isImco;
    private String isOog;
    private String isRefer;
    private String referType;
    private String imcoClassType;
    private String guaranteeFlag;
    private String guaranteedBy;
    private Long extraFreeDays;
    private Long extraFreeDaysPrnpls;
    private String oogLW;
    private String oogRW;
    private String oogF;
    private String oogA;
    private String oogType;
    private String imcoClassActual;
    private LocalDate displayCollectedDate;
    private Long totalDaysCollected;
    private Long totalAmountCollected;
    private String printReturnFormDefault;
    private String printDeliveryFormDefault;
    private String demDttPbleTrnsfd;
    private String hsCode;
    private String hsDescription;
    private Long amountPerDayAfterFree;
    private LocalDate actualDischargeDate;

    // ---- ContainerValidatable interface ----
    @Override public String getContainerNoValue() { return containerNo; }
    @Override public String getEquipmentIsoTypeValue() { return equipmentIsoType; }
}
