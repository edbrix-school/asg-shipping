package com.asg.shipping.shippingofoqv2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OFOQItemDtlDto {


    @Schema(description = "Detail row identifier", example = "1")
    private Long detRowId;

    @Schema(description = "Vessel voyage POID", example = "403592")
    private Long vesselVoyagePoid;

    @Schema(description = "Shipping line name", example = "CORDELIA CONTAINER SHIPPING LINE (CCS)")
    private String lineName;

    @Schema(description = "Vessel name", example = "GSL ELIZABETH")
    private String vesselName;

    @Schema(description = "Voyage number", example = "442")
    private String voyageNo;

    @Schema(description = "Job number", example = "ASG6894")
    private String jobNo;

    @Schema(description = "Arrival date (yyyy-MM-dd)", example = "2024-10-17")
    private LocalDate arrivalDate;

    @Schema(description = "Sail date (yyyy-MM-dd)", example = "2024-10-17")
    private LocalDate sailDate;

    @Schema(description = "Checked flag (legacy usage)", example = "Y")
    private String checked;

    @Schema(description = "Drill-down link metadata", example = "TARGET_DOC_ID=100-101,DOC_KEY_POID=403592")
    private String drillDownLinkInfo;

    @Schema(description = "Shipping line POID", example = "4322")
    private Long linePoid;

    @Schema(description = "Action type (ISCREATE / ISDELETE)", example = "ISCREATE")
    private String actionType;
}