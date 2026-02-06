package com.asg.shipping.shippingofoqv2.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OFOQCheckStatusManifestResponse {

    private Long transactionPoid;
    private Long detRowId;
    private LocalDateTime date;
    private String functionalReference;
    private String StatusCode;
    private String responseMessage;
    private String processingStatus;

}
