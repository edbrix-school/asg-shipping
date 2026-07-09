package com.asg.shipping.importmanifestbl.dto;

import com.asg.shipping.common.dto.LovItem;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChargeOtherDto {

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
}
