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
@Schema(description = "Line Master User Role Detail")
public class UserRoleDetailDto {

    @Schema(description = "Detail Row ID (null for new rows)")
    private Long detRowId;

    @Schema(description = "Row action type", example = "ISUPDATED")
    private String actionType;

    @Schema(description = "User Role POID")
    private Long userRolePoid;

    @Schema(description = "User Role details (from LOV)")
    private LovItem userRoleDet;

    @Schema(description = "Valid Until Date")
    private LocalDate validUntil;
}
