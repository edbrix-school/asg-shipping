package com.asg.shipping.receipts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TaxConfig {

    private Long taxPoid;
    private BigDecimal percentage;
    private String taxApplicable;
    private String parameterValue;
}
