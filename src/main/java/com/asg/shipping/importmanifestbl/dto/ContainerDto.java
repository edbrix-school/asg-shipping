package com.asg.shipping.importmanifestbl.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContainerDto {

    private Long detRowId;
    private String socType;
    private String containerNumber;
    private String sealNumber;

    private Long isoTypePoid;
    private String shortDescription;
    private Long commodityPoid;

    private Double cbm;
    private Double grossWeight;
    private Double netWeight;
    private Double tareWeight;

    private Integer packs;
    private String packsType;

    private String hsCode;
    private String hsDescription;

    private Integer customerDays;
    private Integer principalDays;

    private Boolean imco;
    private Boolean reefer;
    private Boolean oog;

    private Boolean grantFlag;
    private String grantBy;

    private Double amountPerDayAfterFree;

    private LocalDateTime actualDischargeDate;
    private LocalDateTime emptyDate;
    private LocalDateTime collectionDate;

    private Double collectionAmount;
    private Integer collectionDays;

    private Long imcoTypePoid;
    private String imcoNumber;
    private String imcoClassDescription;

    private String rfType;
    private String rfHumidity;
    private String rfVent;
    private String rfTemperature;

    private Long oogTypePoid;
    private String oogBack;
    private String oogLeftWidth;
    private String oogRightWidth;
    private String oogHeight;
    private String oogLength;
    private String oogAdditional;
    private String oogFront;
    private String actionType;
}
