package com.asg.shipping.shippingofoqv2.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Update payload for an OFOQ document. Line details and BL amendment rows live on
 * {@link ShippingOFOQV2RequestDto} so create and update persist the detail tables the same way the
 * legacy screen does - the array table rows are saved together with the header and only then posted.
 */
@Setter
@Getter
@NoArgsConstructor
public class ShippingOFOQV2UpdateRequestDto extends ShippingOFOQV2RequestDto {
}
