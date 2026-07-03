package com.asg.shipping.containerinventorymovementupdate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemurrageCalculateResponse {
    private BigDecimal demurrageAmount;
    private BigDecimal totalCollectedAmount;
    private String collectedSummaryMessage;
    private BigDecimal portDays;

}


