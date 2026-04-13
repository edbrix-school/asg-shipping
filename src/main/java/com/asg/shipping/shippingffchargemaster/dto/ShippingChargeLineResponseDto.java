package com.asg.shipping.shippingffchargemaster.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
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
