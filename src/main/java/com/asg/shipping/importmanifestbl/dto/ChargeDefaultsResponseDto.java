package com.asg.shipping.importmanifestbl.dto;

import com.asg.shipping.common.dto.LovItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargeDefaultsResponseDto {
    private Long taxPoid;
    private BigDecimal taxPercentage;
    private LovItem taxDet;
}
