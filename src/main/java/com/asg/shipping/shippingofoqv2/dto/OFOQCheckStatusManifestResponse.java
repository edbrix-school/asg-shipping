package com.asg.shipping.shippingofoqv2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OFOQCheckStatusManifestResponse {

    private Long transactionPoid;
    private Long detRowId;
    private LocalDate date;
    private String functionalReference;
    private String StatusCode;
    private String responseMessage;
    private String processingStatus;

}
