package com.asg.shipping.lineprincipalmaster.dto;

import com.asg.shipping.common.dto.LovItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Line Master PIC Detail")
public class PicDetailDto {

    @Schema(description = "Detail Row ID (null for new rows)")
    private Long detRowId;

    @Schema(description = "Department POID")
    private Long departmentPoid;

    @Schema(description = "Department details (from LOV)")
    private LovItem departmentDet;

    @Schema(description = "Handled User POID")
    private Long handledUserPoid;

    @Schema(description = "Handled User details (from LOV)")
    private LovItem handledUserDet;

    @Schema(description = "Period From Date")
    private LocalDate periodFrom;

    @Schema(description = "Period To Date")
    private LocalDate periodTo;

    @Schema(description = "Remarks")
    private String remarks;
}
