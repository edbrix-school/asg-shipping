package com.asg.shipping.shippingofoqv2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ShippingOFOQV2UpdateRequestDto extends  ShippingOFOQV2RequestDto {


    @Schema(description = "OFOQ line item details")
    private List<OFOQItemDtlDto> lineDetails;

    @Schema(description = "BL amendment details ")
    private List<OFOQRequestAmendBlDto> amendBl;


}
