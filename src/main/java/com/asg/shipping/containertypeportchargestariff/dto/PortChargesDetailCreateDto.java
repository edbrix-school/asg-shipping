package com.asg.shipping.containertypeportchargestariff.dto;

import com.asg.shipping.containertypeportchargestariff.validation.AtLeastOnePositiveAmount;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AtLeastOnePositiveAmount
public class PortChargesDetailCreateDto {

    private Long detRowId;

    @NotNull(message = "Charge code POID is required")
    private Long chargeCodePoid;

    @NotBlank(message = "Charge type applicable is required")
    @Size(max = 25, message = "Charge type applicable cannot exceed 25 characters")
    private String chargeTypeApplicable;

    @NotBlank(message = "Charge applicable is required")
    @Size(max = 25, message = "Charge applicable cannot exceed 25 characters")
    private String chargeApplicable;

    @Size(max = 25, message = "IMCO class type cannot exceed 25 characters")
    private String imcoClassType;

    @Size(max = 25, message = "OOG type cannot exceed 25 characters")
    private String oogType;

    @Size(max = 25, message = "Others type cannot exceed 25 characters")
    private String othersType;

    private BigDecimal amount20;
    private BigDecimal amount40;
    private BigDecimal amountOther;
    private BigDecimal amount20Cost;
    private BigDecimal amount40Cost;
    private BigDecimal amountOtherCost;
    private BigDecimal amount53;
    private BigDecimal amount53Cost;

    @NotBlank(message = "Ship charge type is required")
    @Size(max = 100, message = "Ship charge type cannot exceed 100 characters")
    private String shipChargeType;
}