package com.asg.shipping.vesseltypemaster.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for Vessel Type details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VesselTypeDto {

    private Long vesselTypePoid;
    private Long groupPoid;
    private String vesselTypeCode;
    private String vesselTypeName;
    private String vesselTypeName2;
    private String active;
    private Integer seqno;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private String deleted;
    private Long costCentrePoid;
    private com.asg.common.lib.dto.LovGetListDto costCentreDet;
}
