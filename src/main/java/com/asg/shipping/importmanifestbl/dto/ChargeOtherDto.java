package com.asg.shipping.importmanifestbl.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChargeOtherDto {

    private Long detRowId;
    private Long chargePoid;
    private String basis;
    private Long chargeTypePoid;

    private Integer quantity;
    private String currencyCode;
    private Double exchangeRate;

    private Double buy;
    private Double sell;

    private Long freightTypePoid;
    private Long paidAtPortPoid;
}

