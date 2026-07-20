package com.asg.shipping.lineprincipalmaster.dto;

import com.asg.shipping.common.dto.LovItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for copying charges from another line
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Copy charges request")
public class CopyChargesRequestDto {

    @NotNull(message = "Source Line POID is mandatory")
    @Schema(description = "Source Line POID (line to copy charges from)", required = true, example = "12346")
    private Long sourceLinePoid;

    private LovItem sourceLineDet;
}

