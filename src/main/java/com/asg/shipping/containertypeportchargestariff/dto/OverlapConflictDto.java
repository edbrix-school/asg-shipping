package com.asg.shipping.containertypeportchargestariff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverlapConflictDto {
    private Long transactionPoid;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private String description;
}