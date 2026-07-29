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
    /** Numeric HTTP status code, e.g. "200". */
    private String statusCode;
    /** HTTP reason phrase, e.g. "OK" - this is what the legacy bean stores in RESPONSE_MSG. */
    private String statusText;
    /** Message extracted from the {@code <Message>} element of the XML body. */
    private String responseMessage;
    private String responseBody;
}
