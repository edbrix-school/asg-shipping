package com.asg.shipping.importmanifestbl.dto;

import com.asg.shipping.common.dto.LovItem;
import com.asg.shipping.importmanifestupdate.service.BlManifestValidationService;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChargeOtherDto implements BlManifestValidationService.ChargeValidatable {

    private Long detRowId;
    private Long chargePoid;
    private String chargeType;
    private LovItem chargeTypeDet;
    private String basis;
    private Long chargeTypePoid;
    private LovItem chargeDet;
    private LovItem basisDet;

    private BigDecimal quantity;
    private String currencyCode;
    private LovItem currencyCodeDet;
    private BigDecimal exchangeRate;

    private BigDecimal buy;
    private BigDecimal sell;

    private String freightType;
    private Long freightTypePoid;
    private LovItem freightTypeDet;
    private Long paidAtPortPoid;
    private LovItem paidAtPortDet;

    @Override public Long getChargePoidValue() { return chargePoid; }
    @Override public String getFreightTypeValue() { return freightType; }
    @Override public BigDecimal getQuantityValue() { return quantity; }
    @Override public BigDecimal getSellValue() { return sell; }
    @Override public BigDecimal getBuyValue() { return buy; }
}
