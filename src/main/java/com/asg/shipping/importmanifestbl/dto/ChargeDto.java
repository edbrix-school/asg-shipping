package com.asg.shipping.importmanifestbl.dto;

import com.asg.shipping.common.dto.LovItem;
import com.asg.shipping.importmanifestupdate.service.BlManifestValidationService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
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
    private LovItem chargeTypeDet;
    private String basisPoid;
    private LovItem chargeDet;
    private LovItem basisDet;

    private String currencyCode;
    private LovItem currencyCodeDet;
    private BigDecimal rate;
    @NotNull
    private BigDecimal quantity;
    private BigDecimal buy;
    private BigDecimal buyAmount;

    private BigDecimal sell;
    private BigDecimal sellAmount;

    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private BigDecimal gain;
    @NotBlank
    private String freightType;
    private LovItem freightTypeDet;
    private Long paidAtPortPoid;
    private LovItem paidAtPortDet;
    private Long receiptInvoicePoid;
    private LovItem receiptInvoiceDet;

    private String chargeDescription;
    private Long taxPoid;
    private LovItem taxDet;
    private Long discountPercentage;

    private LocalDateTime demurrageChargesTillDate;
    private String actionType;

    @Override public Long getChargePoidValue() { return chargePoid; }
    @Override public String getFreightTypeValue() { return freightType; }
    @Override public BigDecimal getQuantityValue() { return quantity; }
    @Override public BigDecimal getSellValue() { return sell; }
    @Override public BigDecimal getBuyValue() { return buy; }

}
