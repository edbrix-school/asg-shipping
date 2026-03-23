package com.asg.shipping.chargegroupmaster.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargeGroupMasterRequestDto {

    @NotBlank(message = "Charge Group Code is mandatory")
    private String chargeGroupCode;

    @NotBlank(message = "Charge Group Name is mandatory")
    private String chargeGroupName;

    private String chargeGroupName2;

    private Long chargeGlPayable;
    private Long chargeGlSale;
    private Long chargeGlCostSale;

    private String linewisePayablePosting;
    private Long seqNo;
    private String active;
}
