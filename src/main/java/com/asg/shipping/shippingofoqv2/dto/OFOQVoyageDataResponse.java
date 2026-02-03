package com.asg.shipping.shippingofoqv2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OFOQVoyageDataResponse {
    
    private OFOQApiDataHdrDto header;
    private List<OFOQItemDtlDto> lineDetail;
    private List<OFOQCheckStatusManifestResponse> manifestResponse;
    private List<OFOQAmendBLDto> amendBL;
    private List<OFOQManifestAmendmentResponse> manifestAmendmentResponse;


}