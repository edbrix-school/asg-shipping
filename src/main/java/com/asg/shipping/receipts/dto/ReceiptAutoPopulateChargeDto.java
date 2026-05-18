package com.asg.shipping.receipts.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReceiptAutoPopulateChargeDto {

    private Long blPoid;
    private LovGetListDto blDet;
    private Long chargePoid;
    private LovGetListDto chargeDet;
    private Long detRowId;
    private BigDecimal amount;
    private String addFlag;
    private String invoiceType;
    private Long taxPoid;
    private LovGetListDto taxDet;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
}
