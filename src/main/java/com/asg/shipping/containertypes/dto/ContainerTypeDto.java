package com.asg.shipping.containertypes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for Container Type details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContainerTypeDto {

    private Long containerTypePoid;
    private Long groupPoid;
    private String containerTypeCode;
    private String containerTypeName;
    private String containerTypeSize;
    private String containerTypeIsoName;
    private Integer containerCargoWeight;
    private Integer containerTareWeight;
    private Integer containerTeuFactor;
    private String containerTypeCategory;
    private Long containerGrpPoid;
    private String containerApmtTypeCode;
    private String active;
    private Integer seqno;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private String deleted;
}


