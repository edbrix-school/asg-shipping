package com.asg.shipping.shippingofoqv2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OFOQRequestAmendBlDto {

    @Schema(description = "Detail row identifier", example = "1")
    private Long detRowId;

    @Schema(description = "Bill of Lading number", example = "CSS24MUNBAH048883")
    private String blNumber;
}
