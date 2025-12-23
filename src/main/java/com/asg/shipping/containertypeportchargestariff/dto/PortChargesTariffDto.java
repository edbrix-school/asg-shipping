package com.asg.shipping.containertypeportchargestariff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortChargesTariffDto {

    private Long transactionPoid;
    private LocalDate transactionDate;
    private Long groupPoid;
    private Long portPoid;
    private String description;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private String deleted;
    private String docRef;
    private Integer seqNo;
    private Long chargeLinePoid;
    private String chargeDivision;
    
    private List<PortChargesDetailDto> details;
}