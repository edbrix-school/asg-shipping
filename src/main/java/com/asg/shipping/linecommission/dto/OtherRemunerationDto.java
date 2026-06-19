package com.asg.shipping.linecommission.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtherRemunerationDto {
    private Long detRowId; // for update
    private String actionType;
    private Long remunerationPoid;
    private LovGetListDto remunerationDet;
    private Long currencyPoid;
    private LovGetListDto currencyDet;
    private BigDecimal percent;
    private BigDecimal ourBookingPercentage;
    private BigDecimal principalBookPutClt;
    private BigDecimal expImpPreCollection;
    private BigDecimal stLegAmount;
    private BigDecimal splEqpPercentage;
    private BigDecimal amount;
    private BigDecimal amountPerTeu;
    private BigDecimal paybackPercent;
    private String remarks;
    private String createdBy;
    private LocalDateTime createdDate;
    private String updatedBy;
    private LocalDateTime updatedDate;
}


