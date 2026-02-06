package com.asg.shipping.shippingofoqv2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OFOQCheckStatusResponseDto {

    private OFOQApiDataHdrDto header;
    private List<OFOQCheckStatusManifestResponse> manifestResponses;



}
