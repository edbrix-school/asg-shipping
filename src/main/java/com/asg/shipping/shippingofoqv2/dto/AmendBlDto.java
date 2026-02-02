package com.asg.shipping.shippingofoqv2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AmendBlDto {

    private OFOQAmendBLDto amendBL;
    private List<OFOQManifestAmendmentResponse> manifestAmendmentResponses;
 }
