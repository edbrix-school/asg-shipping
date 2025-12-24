package com.asg.shipping.containertypeportchargestariff.entity;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShipPortChargesDtlId implements Serializable {
    private Long transactionPoid;
    private Long detRowId;
}
