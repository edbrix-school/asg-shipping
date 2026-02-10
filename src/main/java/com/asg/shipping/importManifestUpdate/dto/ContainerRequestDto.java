package com.asg.shipping.importManifestUpdate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerRequestDto {

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
    private LocalDateTime issueToConsignee;
    private LocalDateTime returnFromConsignee;
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
    private LocalDateTime displayCollectedDate;
    private Long totalDaysCollected;
    private Long totalAmountCollected;
    private String printReturnFormDefault;
    private String printDeliveryFormDefault;
    private String demDttPbleTrnsfd;
    private String hsCode;
    private String hsDescription;
    private Long amountPerDayAfterFree;
    private LocalDateTime actualDischargeDate;

}
