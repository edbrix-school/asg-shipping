package com.asg.shipping.chargegroupmaster.dto;

import com.asg.common.lib.dto.LovGetListDto;
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
    private LovGetListDto groupDet;
    private String chargeGroupCode;
    private String chargeGroupName;
    private String chargeGroupName2;

    private Long chargeGlPayable;
    private LovGetListDto chargeGlPaybeDet;
    private Long chargeGlSale;
    private LovGetListDto chargeGlSaleDet;
    private Long chargeGlCostSale;
    private LovGetListDto chargeGlCostSaleDet;

    private String linewisePayablePosting;
    private String active;
    private Long seqNo;
    private String glPrefix;

    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
