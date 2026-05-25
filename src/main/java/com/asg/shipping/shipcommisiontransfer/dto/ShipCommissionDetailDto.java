package com.asg.shipping.shipcommisiontransfer.dto;


import com.asg.common.lib.dto.LovGetListDto;
import com.asg.shipping.common.dto.LovItem;
import com.asg.shipping.shipcommisiontransfer.enums.ActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for SHIP_BL_COMMISSION_DTL
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipCommissionDetailDto {

    private Long detRowId;
    private String description;
    private Long blTransactionPoid;
    private LovGetListDto blTransactionPoidDet; // LOV data
    private String currencyCode;
    private BigDecimal currencyExchange;
    private BigDecimal quantity20;
    private BigDecimal quantity40;
    private BigDecimal buyPercharge;
    private BigDecimal sellAmount;
    private BigDecimal commissionAmt;
    private String freightType;
    private LovItem freightTypeDet; // LOV data
    private String selected;
    private BigDecimal commitionOnAmount;
    private BigDecimal buyPerchargeFrt;
    private BigDecimal sellAmountFrt;
    private String drilldownLinkInfo;
    private String blType;
    private LovItem blTypeDet; // LOV data
    private String blStatus;
    private LovItem blStatusDet; // LOV data
    private BigDecimal thcAmount;
    private BigDecimal commissionHandAmt;
    private BigDecimal commissionAdjAmt;
    private String shortLegSelected;
    private ActionType actionType;
}
