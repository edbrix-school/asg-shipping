package com.asg.shipping.groupcontainertypes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for Container Group details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContainerGroupDto {

    private Long containerGrpPoid;
    private Long groupPoid;
    private String containerGrpCode;
    private String containerGrpName;
    private String active;
    private Integer seqno;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private String deleted;
}

