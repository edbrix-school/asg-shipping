package com.asg.shipping.containertypeportchargestariff.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateOverlapRequestDto {
    private Long transactionPoid; // Optional - exclude this ID from overlap check
    
    @NotNull
    private Long portPoid;
    
    @NotNull
    private Long chargeLinePoid;
    
    @NotNull
    private String chargeDivision;
    
    @NotNull
    private LocalDate periodFrom;
    
    @NotNull
    private LocalDate periodTo;
}