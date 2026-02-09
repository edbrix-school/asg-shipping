package com.asg.shipping.shippingofoqv2.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OFOQManifestAmendmentResponse {

    private Long detRowId;
    private LocalDateTime date;
    private String blNumber;
    private String functionalReference;
    private Long statusCode;
    private String responseMessage;
    private String processingStatus;
}
