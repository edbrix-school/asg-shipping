package com.asg.shipping.shippingofoqv2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OFOQBlDtlDto {

    private String drillDownLinkInfo;
    private Long companyPoid;
    private Long manifestPoid;
    private String manifestDocRef;
    private String blNumber;
}
