package com.asg.shipping.chargegroupmaster.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargeGroupMasterResponseDto {
    private Long chargeGroupPoid;
    private Long groupPoid;

    private String chargeGroupCode;
    private String chargeGroupName;
    private String chargeGroupName2;

    private Long chargeGlPayable;
    private Long chargeGlSale;
    private Long chargeGlCostSale;

    private String linewisePayablePosting;
    private String active;
    private Long seqNo;
    private String glPrefix;

    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
