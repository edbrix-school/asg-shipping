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
    private String chargeType;
    private String basis;
    private Long chargeTypePoid;

    private Long quantity;
    private String currencyCode;
    private Long exchangeRate;

    private Long buy;
    private Long sell;

    private Long freightTypePoid;
    private Long paidAtPortPoid;
}

