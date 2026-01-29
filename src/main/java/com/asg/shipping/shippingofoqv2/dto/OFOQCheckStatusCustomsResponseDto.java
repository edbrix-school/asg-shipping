package com.asg.shipping.shippingofoqv2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OFOQCheckStatusCustomsResponseDto {

    private String functionalReference;
    private String statusCode;
    private String responseMessage;
    private String responseBody;
}
