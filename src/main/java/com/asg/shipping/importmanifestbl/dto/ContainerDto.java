package com.asg.shipping.importmanifestbl.dto;

import com.asg.shipping.common.dto.LovItem;
import com.asg.shipping.importmanifestupdate.service.BlManifestValidationService;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContainerDto implements BlManifestValidationService.ContainerValidatable {

    private Long detRowId;
    private String socType;
    private String containerNumber;
    private String sealNumber;

    private String equipmentIsoType;
    private LovItem equipmentIsoTypeDet;
    private String shortDescription;
    private Long commodityPoid;
    private LovItem commodityDet;

    private Long cbm;
    private Long grossWeight;
    private Long netWeight;
    private Long tareWeight;

    private Long packs;
    private String packsType;

    private String hsCode;
    private String hsDescription;

    private Long customerDays;
    private Long principalDays;

    private Boolean imco;
    private Boolean reefer;
    private Boolean oog;

    private Boolean grantFlag;
    private String grantBy;

    private Long amountPerDayAfterFree;

    private LocalDate actualDischargeDate;
    private LocalDate emptyDate;
    private LocalDate collectionDate;

    private Long collectionAmount;
    private Long collectionDays;

    private String imcoType;
    private String imcoNumber;
    private String imcoClassDescription;

    private String rfType;
    private String rfHumidity;
    private String rfVent;
    private String rfTemperature;

    private String oogType;
    private String oogBack;
    private String oogLeftWidth;
    private String oogRightWidth;
    private String oogHeight;
    private String oogLength;
    private String oogAdditional;
    private String oogFront;
    private String actionType;

    @Override public String getContainerNoValue() { return containerNumber; }
    @Override public String getEquipmentIsoTypeValue() { return equipmentIsoType; }
}
