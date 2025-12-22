package com.asg.shipping.shippingFFChargeMaster.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShippingChargeLineResponseDto {
    private Long chargePoid;
    private Long linePoid;
    private String lineName;
    private String lineCode;
}
