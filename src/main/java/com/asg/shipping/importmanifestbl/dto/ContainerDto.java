package com.asg.shipping.importmanifestbl.dto;

import com.asg.shipping.common.dto.LovItem;
import com.asg.shipping.importmanifestupdate.service.BlManifestValidationService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContainerDto implements BlManifestValidationService.ContainerValidatable {

    private Long detRowId;
    private String socType;
    @NotBlank
    private String containerNumber;
    @NotBlank
    private String sealNumber;

    @NotBlank
    private String equipmentIsoType;
    private LovItem equipmentIsoTypeDet;
    private String shortDescription;
    @NotNull
    private Long commodityPoid;
    private LovItem commodityDet;
    @NotBlank
    private String imcoType;
    private LovItem imcoTypeDet;

    @NotNull
    private BigDecimal cbm;
    @NotNull
    private BigDecimal grossWeight;
    @NotNull
    private BigDecimal netWeight;
    @NotNull
    private BigDecimal tareWeight;

    @NotNull
    private BigDecimal packs;
    @NotBlank
    private String packsType;

    @NotBlank
    private String hsCode;
    @NotBlank
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

    @NotBlank
    private String imcoNumber;
    @NotBlank
    private String imcoClassDescription;

    @NotBlank
    private String rfType;
    @NotBlank
    private String rfHumidity;
    @NotBlank
    private String rfVent;
    @NotBlank
    private String rfTemperature;

    @NotBlank
    private String oogType;
    private LovItem oogTypeDet;
    @NotBlank
    private String oogBack;
    @NotBlank
    private String oogLeftWidth;
    @NotBlank
    private String oogRightWidth;
    @NotBlank
    private String oogHeight;
    @NotBlank
    private String oogLength;
    @NotBlank
    private String oogAdditional;
    @NotBlank
    private String oogFront;
    private String actionType;

    @Override public String getContainerNoValue() { return containerNumber; }
    @Override public String getEquipmentIsoTypeValue() { return equipmentIsoType; }
}
