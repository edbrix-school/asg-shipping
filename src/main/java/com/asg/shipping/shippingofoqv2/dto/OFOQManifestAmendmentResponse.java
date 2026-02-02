package com.asg.shipping.shippingofoqv2.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OFOQManifestAmendmentResponse {

    private Long detRowId;
    private LocalDate date;
    private String blNumber;
    private String functionalReference;
    private Long statusCode;
    private String responseMessage;
    private String processingStatus;
}
