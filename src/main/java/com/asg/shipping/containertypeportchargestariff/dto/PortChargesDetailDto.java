package com.asg.shipping.containertypeportchargestariff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortChargesDetailDto {

    private Long transactionPoid;
    private Long detRowId;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private Long chargeCodePoid;
    private String chargeTypeApplicable;
    private String chargeApplicable;
    private String imcoClassType;
    private String oogType;
    private String othersType;
    private BigDecimal amount20;
    private BigDecimal amount40;
    private BigDecimal amountOther;
    private BigDecimal amount20Cost;
    private BigDecimal amount40Cost;
    private BigDecimal amountOtherCost;
    private BigDecimal amount53;
    private BigDecimal amount53Cost;
    private String shipChargeType;
}