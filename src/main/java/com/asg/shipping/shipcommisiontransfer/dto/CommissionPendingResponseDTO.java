package com.asg.shipping.shipcommisiontransfer.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CommissionPendingResponseDTO {
    private String targetDocInfo;
    private Long blPoid;
    private LovGetListDto blDet;
    private String currencyCode;
    private BigDecimal currencyExchange;
    private String impExpType;
    private BigDecimal quantity20;
    private BigDecimal quantity40;
    private String blStatus;
    private BigDecimal sellAmount;
    private BigDecimal buyPercharge20;
    private BigDecimal buyPercharge40;
    private BigDecimal sellAmount20;
    private BigDecimal sellAmount40;
    private BigDecimal commissionAmt;
    private BigDecimal buyPercharge;
    private BigDecimal commissionOnAmount;
    private BigDecimal thcAmount;
    private String freightType;
}
