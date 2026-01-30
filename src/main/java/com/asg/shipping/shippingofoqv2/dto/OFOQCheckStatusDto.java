package com.asg.shipping.shippingofoqv2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OFOQCheckStatusDto {
    private String functionalReference;
    private Long transactionPoid;
    private String docReference;
    private String blNumber;
}
