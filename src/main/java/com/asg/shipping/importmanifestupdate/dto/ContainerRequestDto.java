package com.asg.shipping.importmanifestupdate.dto;

import com.asg.shipping.common.dto.LovItem;
import com.asg.shipping.importmanifestupdate.service.BlManifestValidationService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
    private LovItem equipmentIsoTypeDet;
    private String equipmentType;
    private String equipmentSize;
    private Long quantity;
    private Long grsVolume;
    private BigDecimal grsWeight;
    private BigDecimal netVolume;
    private BigDecimal netWeight;
    private BigDecimal tareWeight;
    private BigDecimal noOfPacks;
    private String packUnit;
    private Long comodityPoid;
    private LovItem comodityDet;
    private Long destinationPortPoid;
    private LovItem destinationPortDet;
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
    private LovItem imcoClassTypeDet;
    private String guaranteeFlag;
    private String guaranteedBy;
    private Long extraFreeDays;
    private Long extraFreeDaysPrnpls;
    private String oogLW;
    private String oogRW;
    private String oogF;
    private String oogA;
    private String oogType;
    private LovItem oogTypeDet;
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
