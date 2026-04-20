package com.asg.shipping.lineprincipalmaster.dto;

import com.asg.shipping.common.dto.LovItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for Container Type Detail (used in request and response)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Line Master Container Type Detail")
public class ContainerTypeDetailDto {

    @Schema(description = "Detail Row ID (for updates, null for new container types)")
    private Long detRowId;

    @Schema(description = "Row action type", example = "ISUPDATED")
    private String actionType;

    @Schema(description = "Container Type POID")
    private Long containerTypePoid;

    @Schema(description = "Container Type details (from LOV)")
    private LovItem containerTypeDet;

    @Schema(description = "Valid Until Date")
    private LocalDate validUntil;
}
