package com.asg.shipping.linecommission.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtherRemunerationDto {
    private Long detRowId; // for update
    private Long remunerationPoid;
    private LovGetListDto remunerationDet;
    private Long currencyPoid;
    private LovGetListDto currencyDet;
    private Long percent;
    private Long ourBookingPercentage;
    private Long principalBookPutClt;
    private Long expImpPreCollection;
    private Long stLegAmount;
    private Long splEqpPercentage;
    private Long amount;
    private Long amountPerTeu;
    private Long paybackPercent;
    private String remarks;
}


