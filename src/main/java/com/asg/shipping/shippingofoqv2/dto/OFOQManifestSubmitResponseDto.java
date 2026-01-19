package com.asg.shipping.shippingofoqv2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OFOQManifestSubmitResponseDto {

    private Long detRowId;
    private String functionalRefId;
    private String status;
    private String message;

}