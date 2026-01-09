package com.asg.shipping.shippingmanifestcorrector.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for Shipping Manifest Corrector Charge Detail
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManifestCorrectorChargeDtlDto {

    private Long detRowId;
    private Long chargePoid;
    private LovGetListDto chargeDet;
    private BigDecimal currencyExchange;
    private BigDecimal quantity;
    private BigDecimal buyPercharge;
    private BigDecimal perQuantityAmount;
    private Long paidAtPortPoid;
    private LovGetListDto paidAtPortDet;
    private String chargeType;
    private LovGetListDto chargeTypeDet;
    private String currencyCode;
    private LovGetListDto currencyDet;
    private String equipmentIsoType;
    private String freightType;
    private LovGetListDto freightTypeDet;
    private String ediChargeCode;
    private Long arShReceiptTransactionPoid;
    private String chargeBasisOn;
    private LovGetListDto chargeBasisOnDet;
    private String printGroup;
    private Long receiptInvoicePoid;
    private String docRefLinkNo;
    private String containerNumber;
    private BigDecimal revPayable;
    private BigDecimal revIncome;
}

