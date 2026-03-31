package com.asg.shipping.importmanifestbl.dto;

import com.asg.shipping.importmanifestupdate.service.BlManifestValidationService;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChargeDto implements BlManifestValidationService.ChargeValidatable {

    private Long detRowId;
    private Long chargePoid;
    private String printGroup;
    private String chargeType;
    private Long chargeTypePoid;
    private String basisPoid;

    private String currencyCode;
    private Long rate;

    private Long quantity;
    private Long buy;
    private Long buyAmount;

    private Long sell;
    private Long sellAmount;

    private Long taxPercentage;
    private Long taxAmount;
    private Long gain;

    private String freightType;
    private Long paidAtPortPoid;

    private String chargeDescription;
    private Long taxPoid;
    private Long discountPercentage;

    private LocalDateTime demurrageChargesTillDate;
    private String actionType;

    @Override public Long getChargePoidValue() { return chargePoid; }
    @Override public String getFreightTypeValue() { return freightType; }
    @Override public Long getQuantityValue() { return quantity; }
    @Override public Long getSellValue() { return sell; }
    @Override public Long getBuyValue() { return buy; }

}
