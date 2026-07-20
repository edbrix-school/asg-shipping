package com.asg.shipping.linecommission.dto;

import com.asg.common.lib.dto.LovGetListDto;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContainerRateDto {
    private Long detRowId; // for update
    private String actionType;

    @NotNull(message = "containerTypePoid is required")
    private Long containerTypePoid;
    private LovGetListDto containerTypeDet;

    private BigDecimal importBoxRate;
    private BigDecimal exportBoxRate;
    private BigDecimal transhipBoxRate;
    private BigDecimal shortLegAmount;
    private String remarks;
    private String createdBy;
    private LocalDateTime createdDate;
    private String updatedBy;
    private LocalDateTime updatedDate;
}


