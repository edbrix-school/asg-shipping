package com.asg.shipping.shippingffchargemaster.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargeCreateDTO {

    @Size(max = 20, message = "Charge code must not exceed 20 characters")
    private String chargeCode;

    @Size(max = 100, message = "Charge name must not exceed 100 characters")
    private String chargeName;

    @Size(max = 50, message = "Charge revenue type must not exceed 50 characters")
    private String chargeRevenueType;

    @Size(max = 50, message = "Charge type must not exceed 50 characters")
    private String chargeType;

    @Size(max = 25, message = "Charge applicable type must not exceed 25 characters")
    private String chargeApplicableType;

    @Size(max = 25, message = "Division code must not exceed 25 characters")
    private String divisionCode;

    //private String visibleInFf;

    private Long chargeGlRevenue;
    private Long chargeGlCost;
    private Long chargeGlWip;
    private Long chargePayableGl;
    private Long fdaGlRevenue;
    private Long fdaGlCost;
    private Long directRevenueGl;
    private Long directCostOfSaleGl;
    private Long directPayableGl;
    private Long taxPoid;
    private Long inputTaxPoid;
    private Long chargeGroupPoid;
    private Long shFfChargeMap;
    private Long shFfChargeGlPoid;
   /* private Long shFfChargeGlPoidRev;
    private Long oldChargeGlRevenue;
    private Long oldChargeGlCost;*/

    @Pattern(regexp = "^[YN]$", message = "Active must be Y or N")
    private String active;

    @Positive(message = "Sequence number must be positive")
    private Integer seqno;
}
