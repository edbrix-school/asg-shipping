package com.asg.shipping.shippingofoqv2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OFOQAmendBLDto {

    private Long detRowId;
    private String blNumber;
    private String functionalReference;
    private Long statusCode;
    private String response;
    private String processingStatus;
    private String amendmentRequest;
    private String manifestStatus;


}
